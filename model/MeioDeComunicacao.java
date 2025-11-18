/*****************************************************************
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 20/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: MeioDeComunicacao
* Funcao...........: Transfere a mensagem codificada, aplicando chance de erro por quadro de enquadramento.
*************************************************************** */
package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;
import util.Erro;
import util.Auxiliares;
import java.util.Random;

public class MeioDeComunicacao {
  private TelaPrincipalController controller;
  private Random random;

  //referencias das camadas fisicas
  private CamadaFisicaReceptora fisicaReceptoraA;
  private CamadaFisicaReceptora fisicaReceptoraB;
  private CamadaFisicaTransmissora fisicaTransmissoraA;
  private CamadaFisicaTransmissora fisicaTransmissoraB;

  /**************************************************************
  * Metodo: MeioDeComunicacao
  * Funcao: contrutor que refencia as camadas e prepara elas para comunicacao
  * @param fisicaTransmissoraA | instancia da camada fisica transmissora do host A
  * @param fisicaReceptoraA | instancia da camada fisica receptora do host A
  * @param fisicaTransmissoraB | instancia da camada fisica transmissora do host B
  * @param fisicaReceptoraB | instancia da camada fisica receptora do host B
  * @param controller | instancia do controller
  * @return void 
  * ********************************************************* */
  public MeioDeComunicacao(CamadaFisicaTransmissora fisicaTransmissoraA, CamadaFisicaReceptora fisicaReceptoraA,
  CamadaFisicaTransmissora fisicaTransmissoraB, CamadaFisicaReceptora fisicaReceptoraB, TelaPrincipalController controller) {
    //instancias e sets necessarios para a comunicacao
    this.fisicaReceptoraA = fisicaReceptoraA;
    this.fisicaReceptoraB = fisicaReceptoraB;
    this.fisicaTransmissoraA = fisicaTransmissoraA;
    this.fisicaTransmissoraB = fisicaTransmissoraB;
    this.controller = controller;
    this.random = new Random();
    this.fisicaTransmissoraA.setMeioDeComunicacao(this);
    this.fisicaTransmissoraB.setMeioDeComunicacao(this);
    this.fisicaReceptoraA.setMeioDeComunicacao(this);
    this.fisicaReceptoraB.setMeioDeComunicacao(this);
  } // fim do construtor

  public void transmitir(int fluxoBrutoDeBits[], CamadaFisicaTransmissora destino) throws Erro{
    double taxaErro = this.controller.getValorTaxaErro();
    int[] fluxoBrutoDeBitsPontoInicio = fluxoBrutoDeBits;
    int[] fluxoBrutoDeBitsPontoFim = new int[fluxoBrutoDeBitsPontoInicio.length]; //array destino
    int totalBits = Auxiliares.tratarBits(fluxoBrutoDeBitsPontoInicio);
    int enquad = this.controller.enquadCodification();
    int conErro = this.controller.erroCodification();
    int fluxo = this.controller.fluxoCodification();
    int cod = this.controller.codCodification();
    int tamPosEnlaceEnquad;
    
    //switch para decidir qual tamanho usar baseado no enquadramento
    switch(enquad) {
      case 0: // Contagem de Caracteres (32 bits de dados + 8 bits de cabecalho)
        tamPosEnlaceEnquad = 40;
        break;
      case 1: // Insercao de Bytes e Bits (vamos assumir que 32 bits + 8 da flag de inicio + 8
            // da
            // flag de fim = 48 bits com flags)
      case 2:
        tamPosEnlaceEnquad = 48;
        break;
      case 3: // Violacao da Camada Fisica (passa o subquadro direto)
        tamPosEnlaceEnquad = 40; // O subquadro original tem 32 bits.
        break;
      default:
        tamPosEnlaceEnquad = totalBits;
        break;
    } // fim do switch

    int tamPosEnlaceConErro = tamPosEnlaceEnquad;
    // verificacao para vazio
    if(tamPosEnlaceConErro > 0) {
      switch (conErro) {
        case 0: // Paridade Par
        case 1: // Paridade Impar
          // O TX (BitParidadePar) faz: (totalBits + 1) e alinha para o proximo byte
          int bits = tamPosEnlaceEnquad + 1;
          tamPosEnlaceConErro = (bits + 7) / 8 * 8; // Replica a logica de alinhamento do TX
          break;
        case 2: // CRC
          // O TX (CRC) adiciona 32 bits
          tamPosEnlaceConErro = tamPosEnlaceEnquad + 32;
          break;
        case 3: // Hamming
          // O TX (Hamming) adiciona 'r' bits de paridade
          int quantBitsParidade = 0;
          while ((1 << quantBitsParidade) < (tamPosEnlaceEnquad + quantBitsParidade + 1)) {
            quantBitsParidade++;
          }
          tamPosEnlaceConErro = tamPosEnlaceEnquad + quantBitsParidade;
          tamPosEnlaceConErro = (tamPosEnlaceConErro + 7) / 8 * 8;
          break;
      } // fim switch controle de erro
    } // fim do if

    //calcula o tamanho final com vase no enquadramento
    int tamFinal = tamPosEnlaceConErro;
    if(cod == 1 || cod == 2) {
      tamFinal = tamFinal * 2;
    } //fim do if

    int posErro = -1; //sem erros por enquanto
    
    // loop para gerar erro
    for(int i = 0; i < totalBits; i++) {
      //resetar o erro do quadro anterior
      if(tamFinal > 0 && i % tamFinal == 0) {
        posErro = -1; 

        //sorteia se o quadro atual terá erro
        if(random.nextDouble() < taxaErro) {
          //sorteia a posicao do erro
          int randBit = random.nextInt(tamFinal);
          posErro = i + randBit;
          //garante que o erro nao caia fora do total
          if(posErro >= totalBits) {
            posErro = totalBits - 1;
          } // fim do if
        } // fim do if
      } // fim do if

      // le o bit do A, verifica se eh o que vai ser corrompido, e age
      int bit = Auxiliares.lerBits(fluxoBrutoDeBitsPontoInicio, i , 1);
      
      //verificacao
      if(i == posErro) {
        bit = bit ^ 1; //invertido
      } // fim do if

      Auxiliares.escreverBits(fluxoBrutoDeBitsPontoFim, i, bit, 1);
    } // fim do for

    //verificacao para qual host será mandado
    if(destino == this.fisicaTransmissoraA) {
      int bitsEnviados = Auxiliares.tratarBits(fluxoBrutoDeBitsPontoFim);
      final int[] bitsAnimation = Auxiliares.unpackForAnimation(fluxoBrutoDeBitsPontoFim, bitsEnviados);

      Platform.runLater(() -> {
        this.controller.desenharSinalTransmissao(bitsAnimation);
      });

      this.fisicaReceptoraB.receber(fluxoBrutoDeBitsPontoFim);
    } else if (destino == this.fisicaTransmissoraB){
      this.fisicaReceptoraA.receber(fluxoBrutoDeBitsPontoFim);
    } // fim do if-else
  }// fim do metodo
} // Fim da classe