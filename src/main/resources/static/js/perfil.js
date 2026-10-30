/**
 * Consulta e atualiza os dados cadastrais sem alterar CPF, senha ou saldo.
 * Fluxo: carregar dados → validar a ação → chamar a API → atualizar a interface.
 */
(async () => {
  const obterElemento = (id) => document.getElementById(id);
  const pagina = document.body.dataset.profilePage;
  const editando = pagina === "edit";
  const token = sessionStorage.getItem("token");
  let usuario = null,
    ocupado = false,
    encerrada = false,
    original = "",
    alterado = false;
  const inputs = [
    "perfil-name-input",
    "perfil-phone-input",
    "perfil-email-input",
    "perfil-birth-input",
  ];
  function mensagem(texto, erro = false) {
    obterElemento("perfil-mensagem").textContent = texto;
    obterElemento("perfil-mensagem").dataset.erro = String(erro);
  }
  function lerCamposFormulario() {
    return {
      name: obterElemento("perfil-name-input").value.trim(),
      numeroTelefone: obterElemento("perfil-phone-input").value.replace(/\D/g, ""),
      email: obterElemento("perfil-email-input").value.trim(),
      dataNascimento: obterElemento("perfil-birth-input").value,
    };
  }
  function possuiAlteracoesPendentes() {
    return editando && usuario && JSON.stringify(lerCamposFormulario()) !== original;
  }
  function sair(evento) {
    evento?.preventDefault();
    if (ocupado) return;
    if (
      possuiAlteracoesPendentes() &&
      !window.confirm("Sair e descartar as alterações ainda não salvas?")
    )
      return;
    encerrada = true;
    alterado = false;
    sessionStorage.removeItem("token");
    for (const id of [
      "perfil-nome",
      "perfil-cpf",
      "perfil-email",
      "perfil-telefone",
      "perfil-nascimento",
    ])
      if (obterElemento(id)) obterElemento(id).textContent = "—";
    if (editando) {
      for (const id of [...inputs, "perfil-cpf-input"]) obterElemento(id).value = "";
      obterElemento("perfil-campos").disabled = true;
    }
    window.location.replace("../index.html");
  }
  // Logout por sessão expirada não deve ser impedido por edições locais.
  function tratarSessaoExpirada() {
    ocupado = false;
    alterado = false;
    original = editando && usuario ? JSON.stringify(lerCamposFormulario()) : "";
    sair();
  }
  for (const link of document.querySelectorAll(".logout")) link.addEventListener("click", sair);
  obterElemento("perfil-sair")?.addEventListener("click", sair);
  window.addEventListener("pageshow", (evento) => {
    if (evento.persisted) window.location.reload();
  });
  window.addEventListener("beforeunload", (evento) => {
    if (alterado && !encerrada) {
      evento.preventDefault();
      evento.returnValue = "";
    }
  });
  if (!token) {
    tratarSessaoExpirada();
    return;
  }
  class ErroDaApi extends Error {
    constructor(texto, status) {
      super(texto);
      this.status = status;
    }
  }
  // Centraliza HTTP, autenticação e tratamento das respostas da API.
  async function requisitarApi(caminho, opcoes = {}) {
    const controle = new AbortController();
    const timeout = setTimeout(() => controle.abort(), 15000);
    try {
      const resposta = await fetch(`../${caminho}`, {
        ...opcoes,
        headers: {
          Authorization: `Bearer ${token}`,
          ...(opcoes.body ? { "Content-Type": "application/json" } : {}),
        },
        cache: "no-store",
        signal: controle.signal,
      });
      if (resposta.status === 401) {
        tratarSessaoExpirada();
        throw new ErroDaApi("Sessão expirada. Entre novamente.", 401);
      }
      if (!resposta.ok) {
        const dadosResposta = await resposta.json().catch(() => null);
        const lerCamposFormulario =
          dadosResposta && typeof dadosResposta === "object"
            ? Object.values(dadosResposta)
                .filter((v) => typeof v === "string")
                .join(" ")
            : "";
        throw new ErroDaApi(
          resposta.status < 500
            ? dadosResposta?.message ||
              lerCamposFormulario ||
              `Requisição recusada (HTTP ${resposta.status}).`
            : `Erro no servidor (HTTP ${resposta.status}).`,
          resposta.status,
        );
      }
      return await resposta.json();
    } finally {
      clearTimeout(timeout);
    }
  }
  function erroTexto(erro) {
    return erro.name === "AbortError"
      ? "O servidor demorou para responder."
      : erro instanceof TypeError
        ? "Não foi possível conectar ao servidor."
        : erro.message;
  }
  const telefone = (u) => u.numeroTelefone || u.numeorTelefone || "";
  const cpfFormatado = (v) => (v || "").replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, "$1.$2.$3-$4");
  const dataFormatada = (v) =>
    /^(\d{4})-(\d{2})-(\d{2})$/.test(v || "") ? v.split("-").reverse().join("/") : "Não informada";
  function validarUsuario(u) {
    if (!u || u.id == null || typeof u.name !== "string")
      throw new Error("Resposta de perfil inválida.");
    return u;
  }
  function preencherDados(u) {
    usuario = u;
    if (editando) {
      obterElemento("perfil-name-input").value = u.name || "";
      obterElemento("perfil-cpf-input").value = cpfFormatado(u.cpf);
      obterElemento("perfil-phone-input").value = telefone(u);
      obterElemento("perfil-email-input").value = u.email || "";
      obterElemento("perfil-birth-input").value = u.dataNascimento || "";
      original = JSON.stringify(lerCamposFormulario());
      alterado = false;
    } else {
      obterElemento("perfil-nome").textContent = u.name;
      if (pagina === "profile") {
        obterElemento("perfil-cpf").textContent = cpfFormatado(u.cpf) || "Não informado";
        obterElemento("perfil-email").textContent = u.email || "Não informado";
        obterElemento("perfil-telefone").textContent = telefone(u) || "Não informado";
        obterElemento("perfil-nascimento").textContent = dataFormatada(u.dataNascimento);
      }
    }
  }
  function atualizarControles() {
    obterElemento("perfil-atualizar").disabled = ocupado;
    if (editando) obterElemento("perfil-campos").disabled = ocupado || encerrada || !usuario;
  }
  async function carregar(inicial = false) {
    if (ocupado || encerrada) return;
    if (
      !inicial &&
      possuiAlteracoesPendentes() &&
      !window.confirm("Recarregar e descartar as alterações ainda não salvas?")
    )
      return;
    ocupado = true;
    atualizarControles();
    mensagem("Carregando seus dados...");
    try {
      const u = validarUsuario(await requisitarApi("users/me"));
      if (!encerrada) {
        preencherDados(u);
        mensagem("Dados atualizados.");
      }
    } catch (erro) {
      if (!encerrada)
        mensagem(`${erroTexto(erro)} Use Recarregar dados para tentar novamente.`, true);
    } finally {
      ocupado = false;
      atualizarControles();
    }
  }
  obterElemento("perfil-atualizar").addEventListener("click", () => carregar());
  if (editando) {
    for (const id of inputs)
      obterElemento(id).addEventListener("input", () => {
        alterado = Boolean(possuiAlteracoesPendentes());
      });
    obterElemento("perfil-cancelar").addEventListener("click", () => {
      if (ocupado || !usuario) return;
      if (
        possuiAlteracoesPendentes() &&
        !window.confirm("Descartar as alterações deste formulário?")
      )
        return;
      preencherDados(usuario);
      mensagem("Formulário restaurado para os últimos dados carregados.");
    });
    obterElemento("perfil-form").addEventListener("submit", async (evento) => {
      evento.preventDefault();
      if (ocupado || encerrada || !usuario) return;
      if (!possuiAlteracoesPendentes()) {
        mensagem("Nenhuma alteração para salvar.");
        return;
      }
      const dados = lerCamposFormulario();
      if (!dados.name || !dados.numeroTelefone || !dados.email || !dados.dataNascimento) {
        mensagem("Preencha nome, telefone, e-mail e data de nascimento.", true);
        return;
      }
      ocupado = true;
      atualizarControles();
      mensagem("Salvando alterações...");
      let enviado = false;
      try {
        enviado = true;
        const atualizado = validarUsuario(
          await requisitarApi(`users/${encodeURIComponent(usuario.id)}`, {
            method: "PUT",
            body: JSON.stringify(dados),
          }),
        );
        if (encerrada) return;
        if (String(atualizado.id) !== String(usuario.id))
          throw new Error("Resposta de usuário inesperada.");
        preencherDados(atualizado);
        mensagem("Dados salvos. Suas chaves Pix já registradas permanecem como estavam.");
      } catch (erro) {
        if (!encerrada) {
          const incerto = enviado && (!(erro instanceof ErroDaApi) || erro.status >= 500);
          mensagem(
            `${erroTexto(erro)}${incerto ? " A alteração pode ter sido salva. Recarregue os dados para conferir antes de editar novamente." : ""}`,
            true,
          );
        }
      } finally {
        ocupado = false;
        atualizarControles();
      }
    });
  }
  await carregar(true);
})();
