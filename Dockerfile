
# Usa Java 21 para compilar e executar o simulador
FROM eclipse-temurin:21-jdk

# Pasta de trabalho dentro do container
WORKDIR /app

# Copia o código-fonte para o container
COPY src/Main.java .

# Compila o programa
RUN javac Main.java

# Inicia o simulador
CMD ["java", "Main"]
