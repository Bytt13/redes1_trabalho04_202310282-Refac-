/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 19/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: AplicacaoTransmissora
* Funcao...........: Envia a mensagem captada pelo controller para a proxima camada da aplicacao
*************************************************************** */
package model;

import util.Erro;

public class AplicacaoTransmissora {
    private CamadaAplicacaoTransmissora camadaAplicacaoTransmissora;

  /**************************************************************
  * Metodo: AplicacaoTransmissora
  * Funcao: contrutor com referencia a camada aplicacao tx
  * @param camadaAplicacaoTransmissora | instancia da camadaAplicacaoTransmissora
  * @return void 
  * ********************************************************* */
    public AplicacaoTransmissora(CamadaAplicacaoTransmissora camadaAplicacaoTransmissora) {
      this.camadaAplicacaoTransmissora = camadaAplicacaoTransmissora;
    } // fim do construtor


  /**************************************************************
  * Metodo: transmitir
  * Funcao: inicia a 
  * @param mensagem | mensagem que vai ser transmitida
  * @return void 
  * ********************************************************* */
    public void transmitir(String mensagem) throws Erro {
      // verificacao de mensagem nula para enviar a mensagem
      if(!mensagem.isEmpty() && mensagem != null) {
        this.camadaAplicacaoTransmissora.transmitir(mensagem);
      }// fim do if
    } // fim do metodo
} // Fim da classe