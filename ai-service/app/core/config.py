from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    jwt_secret: str = "myDefaultJwtSecretKeyForDevOnlyPleaseReplaceInProd2026"

    # MinIO
    minio_endpoint: str = "localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin123"
    minio_bucket: str = "gongkao"
    minio_secure: bool = False

    # Backend URL
    backend_url: str = "http://localhost:8080"

    class Config:
        env_prefix = ""
        env_file = ".env"


settings = Settings()
