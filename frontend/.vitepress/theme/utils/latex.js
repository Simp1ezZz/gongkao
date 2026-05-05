import katex from 'katex'

const INLINE_MATH = /\\\((.+?)\\\)/gs
const DISPLAY_MATH = /\\\[(.+?)\\\]/gs

function renderMath(latex, displayMode) {
  try {
    return katex.renderToString(latex.trim(), {
      displayMode,
      throwOnError: false,
      trust: true,
    })
  } catch {
    return displayMode ? `\\[${latex}\\]` : `\\(${latex}\\)`
  }
}

export function renderLatex(html) {
  if (!html) return ''
  return html
    .replace(DISPLAY_MATH, (_, tex) => renderMath(tex, true))
    .replace(INLINE_MATH, (_, tex) => renderMath(tex, false))
}
