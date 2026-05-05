# ai-service/app/core/config.py
import json
from pydantic import BaseModel
from pydantic_settings import BaseSettings


class ModelConfig(BaseModel):
    name: str
    api_url: str
    api_key: str
    model: str
    max_tokens: int = 4096
    supports_vision: bool = False


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    llm_provider: str = "openai-compatible"
    llm_api_url: str = ""
    llm_api_key: str = ""
    llm_model: str = "gpt-4o"
    llm_max_tokens: int = 4096

    # Multi-model config as JSON string from env var
    llm_models: str = "[]"

    # MinIO config
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "gongkao"

    class Config:
        env_prefix = ""
        env_file = ".env"

    def get_models(self) -> list[ModelConfig]:
        models = json.loads(self.llm_models)
        default = ModelConfig(
            name=f"默认({self.llm_model})",
            api_url=self.llm_api_url,
            api_key=self.llm_api_key,
            model=self.llm_model,
            max_tokens=self.llm_max_tokens,
            supports_vision=True,
        )
        return [default] + [ModelConfig(**m) for m in models]


settings = Settings()
