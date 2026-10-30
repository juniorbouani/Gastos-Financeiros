// Usa o endpoint POST /users que já existe no seu projeto.
const formulario = document.getElementById("cadastro-form");
const botao = document.getElementById("cadastro-botao");
const mensagem = document.getElementById("cadastro-mensagem");
const campo = (id) => document.getElementById(id);
let enviando = false;
let cadastrado = false;

function mostrarMensagem(texto, sucesso = false) {
  mensagem.textContent = texto;
  mensagem.dataset.sucesso = String(sucesso);
  mensagem.hidden = false;
}

formulario.addEventListener("submit", async (evento) => {
  evento.preventDefault();
  if (enviando || cadastrado) return;
  mensagem.hidden = true;

  const senha = campo("senhaHash").value;
  if (senha !== campo("confirmarSenha").value) {
    mostrarMensagem("As senhas não coincidem.");
    campo("confirmarSenha").focus();
    return;
  }

  const usuario = {
    name: campo("name").value.trim(),
    cpf: campo("cpf").value.replace(/\D/g, ""),
    numeroTelefone: campo("numeroTelefone").value.replace(/\D/g, ""),
    dataNascimento: campo("dataNascimento").value,
    email: campo("email").value.trim(),
    // O UserService faz o hash no servidor; não transforme a senha aqui.
    senhaHash: senha,
    balance: 0,
  };

  if (usuario.cpf.length !== 11) {
    mostrarMensagem("Informe um CPF com 11 números.");
    return;
  }

  enviando = true;
  botao.disabled = true;
  botao.textContent = "Cadastrando...";
  const controle = new AbortController();
  const timeout = setTimeout(() => controle.abort(), 15000);

  try {
    // Endereço relativo: abra a página pelo Spring Boot.
    const resposta = await fetch("../users", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(usuario),
      signal: controle.signal,
    });
    if (!resposta.ok) {
      const dados = await resposta.json().catch(() => null);
      let texto = "Não foi possível cadastrar. Tente novamente.";
      if ([400, 409].includes(resposta.status)) {
        const campos = dados && typeof dados === "object"
          ? Object.values(dados).filter(valor => typeof valor === "string").join(" ")
          : "";
        texto = dados?.message || campos || texto;
      }
      throw new Error(texto);
    }

    cadastrado = true;
    formulario.reset();
    mostrarMensagem("Conta criada! Você será direcionado para o login.", true);
    botao.textContent = "Conta criada";
    setTimeout(() => window.location.assign("../index.html"), 1500);
  } catch (erro) {
    mostrarMensagem(erro.name === "AbortError"
      ? "O servidor demorou para responder. Antes de repetir, tente entrar com os dados cadastrados: a conta pode ter sido criada."
      : erro instanceof TypeError
        ? "A conexão falhou. Verifique o servidor e tente entrar antes de repetir o cadastro."
        : erro.message);
  } finally {
    clearTimeout(timeout);
    enviando = false;
    if (!cadastrado) {
      botao.disabled = false;
      botao.textContent = "CADASTRAR →";
    }
  }
});
