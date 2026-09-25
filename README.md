# Raças Caninas — Arquivo de Raças

Aplicação web que consulta a [Dog CEO API](https://dog.ceo/dog-api/) e exibe
um exemplar de cada raça cadastrada. Servidor HTTP em Java puro (sem frameworks),
front-end em HTML, CSS e JavaScript sem dependências.

---

## Requisitos

- JDK 17 ou superior
- Windows, Linux ou macOS

Verifique a instalação:

    java -version
    javac -version

---

## Como rodar

### Windows

Duplo-clique em `run.bat` na raiz do projeto.

O script compila as classes em `bin/`, sobe o servidor e abre o navegador
automaticamente em `http://localhost:8080`.

### Linux / macOS

    mkdir -p bin
    javac -encoding UTF-8 -d bin $(find src -name "*.java")
    java -cp bin App

Depois acesse `http://localhost:8080` no navegador.

### Outra porta

    java -Dport=9090 -cp bin App

Se a porta preferida estiver ocupada, o servidor tenta as 20 seguintes
automaticamente antes de desistir.

---

## Estrutura

    .
    ├── run.bat                    script de execução no Windows
    ├── README.md
    ├── bin/                       classes compiladas (.class)
    ├── lib/
    │   └── web/                   front-end servido pelo App
    │       ├── index.html
    │       ├── style.css
    │       └── script.js
    └── src/
        ├── App.java               ponto de entrada, servidor HTTP
        ├── api/
        │   └── DogApi.java        camada de acesso à API externa
        └── service/
            ├── ListaRacas.java    monta a lista de raças
            └── PaginaCachorro.java monta o HTML do cachorro escolhido

---

## Arquitetura

O projeto segue uma divisão em camadas, cada uma com uma responsabilidade única.

**Camada de acesso a dados — `api/DogApi`**

É a única classe que conhece a Dog CEO API. Faz as requisições HTTP, monta
URLs e extrai os dados do JSON de resposta. Nenhuma outra parte do código
sabe que o dog.ceo existe.

**Camada de serviço — `service/`**

Aplica a lógica de negócio sobre os dados brutos da API.

- `ListaRacas` transforma o mapa de raças e sub-raças em uma lista simples
  e ordenada.
- `PaginaCachorro` recebe o nome da raça informado pelo usuário, monta o
  HTML correspondente e pede a URL da imagem à `DogApi`.

**Camada de aplicação — `App`**

Sobe o servidor HTTP nativo do JDK, registra as rotas, serve arquivos
estáticos e conecta os serviços à camada de apresentação.

**Camada de apresentação — `lib/web/`**

HTML, CSS e JavaScript puros. O `script.js` consome os endpoints do
servidor local em vez de bater direto na API externa — assim o back-end
continua sendo o único ponto de contato com o dog.ceo.

---

## Endpoints

### `GET /api/breeds`

Devolve a lista completa de raças e sub-raças em formato JSON.

    $ curl http://localhost:8080/api/breeds
    ["afghan hound","australian shepherd","beagle","husky", ...]

Cada item é uma raça isolada (`"husky"`) ou uma sub-raça combinada com
a raça principal (`"afghan hound"`).

### `GET /api/image?breed=NOME`

Devolve a URL de uma imagem aleatória da raça informada.

    $ curl "http://localhost:8080/api/image?breed=husky"
    {"url":"https://images.dog.ceo/breeds/husky/n02110185_1469.jpg"}

O parâmetro `breed` é obrigatório. Para sub-raças, informe apenas o nome
principal (`hound`, `retriever`, etc.).

### `GET /*`

Serve arquivos estáticos de `lib/web/`. A raiz `/` devolve o `index.html`.

---

## Front-end

A interface segue uma linha editorial: rail lateral fixo com a lista de
raças, painel principal com o nome da raça e a imagem. Sem framework, sem
dependência externa além das fontes do Google.

**Recursos**

- Filtro em tempo real na lista lateral
- Navegação por teclado (`↑`/`↓` e `j`/`k`)
- URL com hash por raça (`#husky`) — link direto para qualquer raça
- Botão para sortear outra imagem da mesma raça
- Modo claro e escuro seguindo a preferência do sistema
- Layout responsivo, com a lista virando carrossel horizontal em telas pequenas

---

## Limitações conhecidas

- O parsing do JSON da API externa usa regex em vez de biblioteca dedicada.
  Funciona porque o formato da Dog CEO API é estável e simples. Se o
  contrato mudar, os métodos `getImage` e `allBreeds` precisam ser revistos.
- O servidor usa o executor padrão do `HttpServer` (uma thread). Suficiente
  para uso local; para produção, trocar por um pool com `Executors`.
- Sem cache entre requisições. Cada chamada a `/api/image` faz uma nova
  requisição à API externa.
