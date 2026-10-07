const elSaldo = document.getElementById("saldo");
const btnToggle = document.getElementById("btn-toggle-saldo");
const imgIcone = document.getElementById("icone-olho");

if (elSaldo && btnToggle && imgIcone) {
  const valorReal = elSaldo.textContent;
  const mascara = "••••••••";
  let saldoVisivel = true;

  const caminhoBase = imgIcone.src.includes("Naover.svg")
    ? imgIcone.src.replace("Naover.svg", "")
    : imgIcone.src.replace("Ver.svg", "");

  btnToggle.addEventListener("click", () => {
    saldoVisivel = !saldoVisivel;

    if (saldoVisivel) {
      elSaldo.textContent = valorReal;
      imgIcone.src = caminhoBase + "Ver.svg";
    } else {
      elSaldo.textContent = mascara;
      imgIcone.src = caminhoBase + "Naover.svg";
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  atualizarDataHora();
  atualizarSaudacao();

  setInterval(atualizarDataHora, 60000);
});

function atualizarDataHora() {
  const elData = document.getElementById("data-atual");
  if (!elData) return;

  const agora = new Date();

  const opcoesData = {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  };

  let dataFormatada = agora.toLocaleDateString("pt-BR", opcoesData);

  dataFormatada =
    dataFormatada.charAt(0).toUpperCase() + dataFormatada.slice(1);

  elData.textContent = dataFormatada;
}

function atualizarSaudacao() {
  const elSaudacao = document.getElementById("saudacao");
  if (!elSaudacao) return;

  const hora = new Date().getHours();
  let textoSaudacao = "Olá";

  if (hora >= 5 && hora < 12) {
    textoSaudacao = "Bom dia";
  } else if (hora >= 12 && hora < 18) {
    textoSaudacao = "Boa tarde";
  } else {
    textoSaudacao = "Boa noite";
  }

  const textoAtual = elSaudacao.textContent;
  const indiceVirgula = textoAtual.indexOf(",");

  if (indiceVirgula !== -1) {
    const nomeUsuario = textoAtual.substring(indiceVirgula);
    elSaudacao.textContent = `${textoSaudacao}${nomeUsuario}`;
  } else {
    elSaudacao.textContent = `${textoSaudacao}!`;
  }
}
