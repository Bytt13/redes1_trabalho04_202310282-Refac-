/***************************************************************** 
* Autor............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 17/11/2025
* Ultima alteracao.: 17/11/2025
* Nome.............: Erro
* Funcao...........: Cuida da amostragem dos erros
*************************************************************** */
package util;

public class Erro extends Exception{
  private String header; // header do alerta
  
  public Erro(String header, String content) {
    super(content); //alerta padrao
    this.header = header;
  } // fim do construtor

  public String getHeader() {
    return header;
  }

  public String getContent() {
    return getMessage();
  }
}
