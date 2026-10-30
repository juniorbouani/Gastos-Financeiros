// O HTML e a API são servidos pelo mesmo Spring Boot.
const formulario = document.getElementById("login-form");
const campoCpf = document.getElementById("cpf");
const campoSenha = document.getElementById("senha");
const botao = document.getElementById("login-botao");
const mensagem = document.getElementById("login-mensagem");
const alternarSenha = document.querySelector(".btn-toggle-eye");
let enviando = false;

alternarSenha.addEventListener("click", () => {
  const mostrar = campoSenha.type === "password";
  campoSenha.type = mostrar ? "text" : "password";
  alternarSenha.setAttribute("aria-label", mostrar ? "Ocultar senha" : "Mostrar senha");
});

formulario.addEventListener("submit", async (evento) => {
  evento.preventDefault(); // Evita recarregar a página antes de consultar a API.
  if (enviando) return;
  mensagem.hidden = true;
  enviando = true;
  botao.disabled = true;
  botao.textContent = "Entrando...";
  const controle = new AbortController();
  const timeout = setTimeout(() => controle.abort(), 15000);

  try {
    sessionStorage.removeItem("token");
    // Os nomes cpf e senha correspondem aos campos de LoginRequest.java.
    const resposta = await fetch("./auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        cpf: campoCpf.value.replace(/\D/g, ""),
        senha: campoSenha.value,
      }),
      signal: controle.signal,
    });

    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
      let texto = "Não foi possível entrar. Tente novamente.";
      if (resposta.status === 401) texto = "CPF ou senha inválidos.";
      if (resposta.status === 429) texto = dados?.message || "Muitas tentativas. Aguarde antes de tentar novamente.";
      if (resposta.status === 400) texto = dados?.cpf || dados?.senha || dados?.message || "Confira os dados informados.";
      throw new Error(texto);
    }

    if (typeof dados?.token !== "string" || !dados.token.trim()) {
      throw new Error("O servidor não retornou um token válido.");
    }

    // Guarda somente o token nesta aba; a senha não é armazenada.
    sessionStorage.setItem("token", dados.token);
    campoSenha.value = "";
    window.location.assign("./PaginaInicial/paginainicial.html");
  } catch (erro) {
    mensagem.textContent = erro.name === "AbortError"
      ? "O servidor demorou para responder. Tente novamente."
      : erro instanceof TypeError
        ? "Não foi possível conectar ao servidor. Abra a página pelo Spring Boot."
        : erro.message;
    mensagem.hidden = false;
  } finally {
    clearTimeout(timeout);
    enviando = false;
    botao.disabled = false;
    botao.innerHTML = "Entrar &rarr;";
  }
});

// Mensagem após alteração de senha ou encerramento de conta, sem armazenar credenciais.
const avisoConta = sessionStorage.getItem("flowbank.accountMessage");
if (avisoConta) {
  sessionStorage.removeItem("flowbank.accountMessage");
  mensagem.textContent = avisoConta;
  mensagem.hidden = false;
}
