# ai-service/app/core/config.py
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    class Config:
        env_prefix = ""
        env_file = ".env"


settings = Settings()
