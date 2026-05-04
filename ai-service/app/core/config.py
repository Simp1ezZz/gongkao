# ai-service/app/core/config.py
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    llm_provider: str = "openai-compatible"
    llm_api_url: str = ""
    llm_api_key: str = ""
    llm_model: str = "gpt-4o"
    llm_max_tokens: int = 4096

    class Config:
        env_prefix = ""
        env_file = ".env"


settings = Settings()
