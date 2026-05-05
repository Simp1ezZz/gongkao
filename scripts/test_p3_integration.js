const BASE = 'http://localhost:8080/api';
let token, sessionId;

async function api(path, opts={}) {
  const url = BASE + path;
  const headers = {'Content-Type': 'application/json', ...opts.headers};
  const res = await fetch(url, {...opts, headers});
  return res.json();
}

function auth(t) { return {'Authorization': 'Bearer ' + t}; }

async function test() {
  let passed = 0, failed = 0;
  function assert(name, condition) {
    if (condition) { passed++; console.log('  PASS: ' + name); }
    else { failed++; console.log('  FAIL: ' + name); }
  }

  console.log('=== P3 Full Integration Test ===\n');

  // 1. Regions
  console.log('--- 1. Region API ---');
  let d = await api('/regions');
  assert('GET /regions returns success', d.success);
  assert('GET /regions returns 32 regions', d.data.length === 32);
  assert('First region is 国考', d.data[0].name === '国考');
  assert('Regions sorted by sortOrder', d.data[0].sortOrder < d.data[1].sortOrder);

  // 2. Paper list
  console.log('\n--- 2. Paper List API ---');
  d = await api('/papers?category=%E8%A1%8C%E6%B5%8B&page=1&pageSize=10');
  assert('GET /papers returns success', d.success);
  assert('GET /papers returns 5 papers', d.data.total === 5);
  assert('Papers have regionName', d.data.list[0].regionName !== undefined);

  // 3. Region filter
  console.log('\n--- 3. Region Filter ---');
  d = await api('/papers?category=%E8%A1%8C%E6%B5%8B&regionId=1');
  assert('regionId=1 filters to 2 papers (国考)', d.data.total === 2);
  assert('All filtered papers are 国考', d.data.list.every(p => p.regionName === '国考'));

  // 4. Paper detail
  console.log('\n--- 4. Paper Detail API ---');
  d = await api('/papers/1');
  assert('GET /papers/1 returns success', d.success);
  assert('Paper has title', d.data.title.includes('国家公务员'));
  assert('Paper has 10 questions', d.data.questions.length === 10);
  assert('Paper has 1 material group', d.data.materials.length === 1);
  assert('Questions do NOT leak answer', !('answer' in d.data.questions[0]));
  assert('Questions do NOT leak explanation', !('explanation' in d.data.questions[0]));
  assert('Questions have options', d.data.questions[0].options !== undefined);
  assert('Questions have module', d.data.questions[0].module !== undefined);

  // 5. Materials
  console.log('\n--- 5. Materials API ---');
  d = await api('/papers/1/materials');
  assert('GET /papers/1/materials returns success', d.success);
  assert('Returns 1 material group', d.data.length === 1);
  assert('Material has content', d.data[0].content !== undefined);

  // 6. Knowledge query
  console.log('\n--- 6. Knowledge Query API ---');
  d = await api('/papers/questions/by-knowledge?module=%E8%A8%80%E8%AF%AD%E7%90%86%E8%A7%A3&limit=5');
  assert('Knowledge query returns success', d.success);
  assert('Returns up to 5 questions', d.data.length <= 5);
  assert('All questions are 言语理解', d.data.every(q => q.module === '言语理解'));

  // 7. Login
  console.log('\n--- 7. Auth ---');
  d = await api('/auth/login-password', {
    method: 'POST',
    body: JSON.stringify({email:'test@test.com', password:'123456'})
  });
  assert('Login returns success', d.success);
  assert('Login returns accessToken', d.data.accessToken !== undefined);
  token = d.data.accessToken;

  // 8. Create session
  console.log('\n--- 8. Session Create ---');
  d = await api('/sessions', {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({paperId: 5})
  });
  assert('Create session returns success', d.success);
  assert('Session status is ongoing', d.data.status === 'ongoing');
  assert('Session has id', d.data.id !== undefined);
  sessionId = d.data.id;

  // 9. Session resume
  console.log('\n--- 9. Session Resume ---');
  d = await api('/sessions', {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({paperId: 5})
  });
  assert('Repeat create returns same session', d.data.id === sessionId);
  assert('Session still ongoing', d.data.status === 'ongoing');

  // 10. Session pause
  console.log('\n--- 10. Session Pause ---');
  d = await api('/sessions/' + sessionId, {
    method: 'PUT',
    headers: auth(token),
    body: JSON.stringify({status: 'paused', timeElapsed: 300, currentIndex: 5})
  });
  assert('Pause returns success', d.success);
  assert('Status changed to paused', d.data.status === 'paused');
  assert('Time elapsed saved', d.data.timeElapsed === 300);
  assert('Current index saved', d.data.currentIndex === 5);

  // 11. Session resume
  console.log('\n--- 11. Session Resume ---');
  d = await api('/sessions/' + sessionId, {
    method: 'PUT',
    headers: auth(token),
    body: JSON.stringify({status: 'ongoing'})
  });
  assert('Resume returns success', d.success);
  assert('Status changed to ongoing', d.data.status === 'ongoing');
  assert('Time elapsed preserved', d.data.timeElapsed === 300);

  // 12. Session submit
  console.log('\n--- 12. Session Submit ---');
  d = await api('/sessions/' + sessionId + '/submit', {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({timeElapsed: 600, answers: '[]'})
  });
  assert('Submit returns success', d.success);
  assert('Status changed to submitted', d.data.status === 'submitted');

  // 13. Batch submit with correct + wrong
  console.log('\n--- 13. Batch Submit + Auto Grading ---');
  d = await api('/sessions', {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({paperId: 1})
  });
  const sid = d.data.id;

  d = await api('/papers/user-answers/batch', {
    method: 'POST',
    headers: auth(token),
    body: JSON.stringify({
      sessionId: sid,
      answers: [
        {questionId: 1, answer: 'B'},  // correct
        {questionId: 2, answer: 'B'},  // wrong (A)
        {questionId: 3, answer: 'B'},  // correct
        {questionId: 4, answer: 'A'},  // wrong (C)
        {questionId: 5, answer: 'A'},  // correct
        {questionId: 6, answer: 'X'},  // wrong (B)
      ]
    })
  });
  assert('Batch submit returns success', d.success);
  assert('Total questions = 6', d.data.totalQuestions === 6);
  assert('Correct count = 3', d.data.correctCount === 3);
  assert('Wrong count = 3', d.data.wrongCount === 3);
  assert('Accuracy = 50%', d.data.accuracy === 50.0);
  assert('Result has questions array', d.data.questions.length === 6);
  assert('Questions have answer field', d.data.questions[0].answer !== undefined);
  assert('Questions have explanation field', d.data.questions[0].explanation !== undefined);
  assert('Q1 isCorrect=true', d.data.questions.find(q=>q.id===1).isCorrect === true);
  assert('Q2 isCorrect=false', d.data.questions.find(q=>q.id===2).isCorrect === false);

  // 14. My answers
  console.log('\n--- 14. My Answers ---');
  d = await api('/papers/1/my-answers', {headers: auth(token)});
  assert('My answers returns success', d.success);
  assert('My answers has records', d.data.length > 0);
  const firstAnswer = d.data[0];
  assert('Answer has userAnswer field', firstAnswer.userAnswer !== undefined);
  assert('Answer has correct answer field', firstAnswer.answer !== undefined);
  assert('Answer has isCorrect field', firstAnswer.isCorrect !== undefined);
  assert('Answer has explanation', firstAnswer.explanation !== undefined);

  // 15. Unauth access
  console.log('\n--- 15. Auth Gate ---');
  d = await api('/sessions', {method: 'POST', body: JSON.stringify({paperId: 1})});
  assert('Unauth session create returns 401', d.code === 401);

  d = await api('/papers/user-answers/batch', {
    method: 'POST',
    body: JSON.stringify({sessionId: 1, answers: []})
  });
  assert('Unauth batch submit returns 401', d.code === 401);

  // 16. Edge cases
  console.log('\n--- 16. Edge Cases ---');
  d = await api('/papers/999');
  assert('Non-existent paper returns error', d.success === false);

  d = await api('/papers?category=%E8%A1%8C%E6%B5%8B&page=99');
  assert('Page beyond data returns empty list', d.data.list.length === 0);

  // Summary
  console.log('\n=== Test Results ===');
  console.log('Passed: ' + passed);
  console.log('Failed: ' + failed);
  console.log('Total:  ' + (passed + failed));
  process.exit(failed > 0 ? 1 : 0);
}

test().catch(e => { console.error('Fatal:', e); process.exit(1); });
