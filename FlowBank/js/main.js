const elSaldo = document.getElementById("saldo");
const btnToggle = document.getElementById("btn-toggle-saldo");
const imgIcone = document.getElementById("icone-olho");

const valorReal = elSaldo.textContent;
const mascara = "••••••••";

let saldoVisivel = true;

btnToggle.addEventListener("click", () => {
  saldoVisivel = !saldoVisivel;

  if (saldoVisivel) {
    elSaldo.textContent = valorReal;
    imgIcone.src = "../img/Ver.svg";
  } else {
    elSaldo.textContent = mascara;
    imgIcone.src = "../img/Naover.svg";
  }
});
