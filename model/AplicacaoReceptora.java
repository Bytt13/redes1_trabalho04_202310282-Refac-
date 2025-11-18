/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 22/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: AplicacaoReceptora
* Funcao...........: Mostra a mensagem depois de todo o processo de transferencia
*************************************************************** */

package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;

public class AplicacaoReceptora {
  private TelaPrincipalController controller; //referencia a UI

  /**************************************************************
  * Metodo: AplicacaoReceptora
  * Funcao: contrutor com referencia a GUI
  * @param controller | instancia do controller
  * @return void 
  * ********************************************************* */
  public AplicacaoReceptora(TelaPrincipalController controller) {
    this.controller = controller;
  } // fim do metodo

  /**************************************************************
  * Metodo: receber
  * Funcao: exibe a mensagem
  * @param mensagem | mensagem que quer enviar
  * @return void 
  * ********************************************************* */
  public void receber(String mensagem) {
    Platform.runLater(() -> {
      this.controller.exibirMensagemRecebida(mensagem);
    });
  } // fim do metodo
} // Fim da classe