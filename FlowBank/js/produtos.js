const btnComprar = document.getElementById("btnComprar");
const nomeProduto = document.getElementById("nomeProduto").textContent;

btnComprar.addEventListener("click", (event) => {
  event.preventDefault();

  alert(`${nomeProduto} fora de estoque`);

  window.location.href = btnComprar.href;
});