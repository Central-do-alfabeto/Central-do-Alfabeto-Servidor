MODEL_URL="https://alphacephei.com/vosk/models/vosk-model-small-pt-0.3.zip"
MODEL_FILENAME="vosk-model-small-pt-0.3.zip"
ROOT_DIR="vosk-model"
TEMP_DIR="tmp_vosk_download"

echo "Iniciando o download do modelo Vosk..."
rm -rf "$TEMP_DIR" "$ROOT_DIR"
mkdir -p "$TEMP_DIR" "$ROOT_DIR"

curl -L -o "$MODEL_FILENAME" "$MODEL_URL"

echo "Descompactando o modelo..."
unzip -q "$MODEL_FILENAME" -d "$TEMP_DIR"

MODEL_DIR="$TEMP_DIR/$(basename "$MODEL_FILENAME" .zip)"

echo "Copiando arquivos para o diretório do modelo..."
cp -R "$MODEL_DIR"/* "$ROOT_DIR"/

echo "Limpando arquivos temporários..."
rm -rf "$MODEL_FILENAME" "$TEMP_DIR"

echo "Modelo Vosk baixado e configurado com sucesso em $ROOT_DIR."