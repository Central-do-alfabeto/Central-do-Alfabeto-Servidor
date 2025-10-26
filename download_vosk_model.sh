MODEL_URL="https://alphacephei.com/vosk/models/vosk-model-pt-fb-v0.1.1-20220516_2113.zip"
MODEL_FILENAME="vosk-model-pt-fb-v0.1.1-20220516_2113.zip"
TARGET_DIR="src/main/resources/vosk-model"

echo "Iniciando o download do modelo Vosk..."

mkdir -p $TARGET_DIR

curl -L -o $MODEL_FILENAME $MODEL_URL

echo "Descompactando o modelo..."
unzip $MODEL_FILENAME -d $TARGET_DIR

MODEL_DIR=$(basename $MODEL_FILENAME .zip)
mv $TARGET_DIR/$MODEL_DIR/* $TARGET_DIR/

echo "Limpando arquivos temporários..."
rm -rf $MODEL_FILENAME
rm -rf $TARGET_DIR/$MODEL_DIR

echo "Modelo Vosk baixado e configurado com sucesso em $TARGET_DIR."