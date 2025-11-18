/***************************************************************** 
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 18/08/2025
* Ultima alteracao.: 31/10/2025
* Nome.............: Auxiliares
* Funcao...........: Codifica e decodifica bits, e transformas arrays em strings, etc...
*************************************************************** */
package util;

public class Auxiliares {
/**************************************************************
 * Metodo: stringToInt
* Funcao: converte a mensagem para um array de bits (inteiro)
* @param mensagem | mensagem enviada
* @return int[] | pacote de bits
* ********************************************************* */
  public static int[] stringToInt(String mensagem) {
    char[] mensagemEmChar = mensagem.toCharArray(); //converte string pra char[]
    int totalBits = (8 * mensagemEmChar.length); //total de bits da mensagem

    int tam = (totalBits + 31) / 32; //arredonda para cima para não haver erros
    int[] pack = new int[tam]; 
    int cont = 0; //acompanhar os bits

    //loop para percorrer os caracteres da mensagem
    for(char caractere : mensagemEmChar) {
      //loop para percorrer cada bit do caractere
      for(int i = 0; i < 8; i++) {
        int bit = (caractere >> (7 -i)) & 1; // 1 = mascara

        // se o bit for 1, precisamos calcular a posicao
        if(bit == 1) {
          int ind = cont / 32; // calcula o indice que o bit vai alocar
          int pos = 32 - (cont % 32); //calcula a posicao do bit

          pack[ind] = pack[ind] | (1 << pos); // armazena o bit no pacote
        } // fim do if
      }// fim do for
      cont++;
    } // fim do for
    return pack;
  } // fim do metodo

  /**************************************************************
   * Metodo: packToString
  * Funcao: converte o pack de bits para string
  * @param pack | pacote de bits
  * @return String | pacote de bits em string
  * ********************************************************* */
  public static String packToString(int[] pack) {
    int totalBits = tratarBits(pack);
    int totalChar = totalBits / 8;
    char[] caracteres = new char[totalChar];

    //loop para reconstruir os char
    for(int i =0; i < totalChar; i++){
      int valorChar = 0; //incializa o char como 0

      //loop para ler os 8 bits
      for(int j = 0; j < 8; j++) {
        int cont = i * 8 + j;
        int indPack = cont / 32;
        int pos = 31 - (cont % 32);

        //extrai bit do pack
        int bit = (pack[indPack] >> pos) & 1;
        
        //se o bit for 1 adiciona o bit um no valor do char
        if(bit == 1) {
          valorChar = valorChar | (1 << (7 - j));
        } // fim do if
      } // fim do for

      //verificacao de validade do char
      if(valorChar != 0) {
        caracteres[i] = (char) valorChar;
      } // fim do if
    } // fim do for

    String mensagemFinal = new String(caracteres); //cria uma string da mensagem depois do processo de conversao

    return mensagemFinal;
  } // fim do metodo

  /**************************************************************
  * Metodo: showBits
  * Funcao: mostra os bits de um pack em um array
  * @param mensagemBits | mensagem binaria
  * @return String | mensagem em string
  * ********************************************************* */
  public static String showBits(int[] mensagemBits) {
    int mascara = 1 << 31;
    String bitsString = "";
    
    //loop para percorrer a mensagem
    for(int bits : mensagemBits) {
      StringBuilder sb = new StringBuilder(); // cria o objeto de string builder para concatenar strings

      //loop para percorrer a mensagem
      for(int i = 1; i <= 32; i++) {
          sb.append((bits & mascara) == 0 ? "0": "1"); 
          bits = bits << 1;

          // adiciona um espaco a cada 8 bits
          if(i % 8 == 0) {
            sb.append(" "); 
          } // fim do if
      } // fim do for

      bitsString = sb.toString();
    } // fim do for
          
    return bitsString;
  } // fim do metodo

  /**************************************************************
  * Metodo: unpackForAnimation
  * Funcao: tira os bits do pack, para animacao
  * @param pack | pack de bits
  * @param max | total de bits maximos
  * @return String | mensagem em string
  * ********************************************************* */
  public static int[] unpackForAnimation(int[] pack, int max) {
    int qtdBits = 0;
    int totalBytes = pack.length * 4; // numero maximo de bytes do array

    //loop para encontrar o ultimo 1
    for(int i = totalBytes - 1; i >= 0; i--) {
      int currByte = lerBits(pack, i * 8, 8);

      //Se for diferente de 0, ou seja, 1, encontramos o que queriamos
      if(currByte != 0) {
        qtdBits = (i + 1) * 8;
        break;
      } // fim do if
    } // fim do for

    //tratamento de mensagens 0
    if(qtdBits == 0 && max > 0) {
      //assume que o tamanho eh pelo menos 8 bits
      if(lerBits(pack , 0, 8) == 0) {
        qtdBits = 8; //assumindo um caractere NUL
      } // fim do if
    }// fim do if

    int[] fluxo = new int[qtdBits]; // cria o array final com o tamnaho certo de bits

    //loop para percorrer os bits validos e simplificar
    for(int i = 0; i < qtdBits; i++) {
      int ind = i / 32;
      int pos = 31 - (i % 32);

      int bit = (pack[ind] >> pos) & 1;
      fluxo[i] = bit;
    } // fim do for

    return fluxo;
  } // fim do metodo

  /**************************************************************
  * Metodo: escreverBits
  * Funcao: escreve um valor inteiro como uma sequencia de bits em um array
  * @param array | para onde vao os bits escritos
  * @param posInicialBit | posicao inicial para escrita
  * @param valor | valor a ser escrito em bits
  * @param numBits | numero de bits para escrever
  * @return void
  * ********************************************************* */
  public static void escreverBits(int[] array, int posInicialBit, int valor, int numBits) {
    // loop para escrever os bits
    for(int i = 0; i < numBits; i++) {
      int bitEscrito = valor >> (numBits - 1 - i) & 1; // extrai o bit que vai ser escrito
      int posGlobal = posInicialBit + i; // calcula a posicao global do bit que vai ser escrito
      int indPack = posGlobal / 32;
      int posPack = 31 - (posGlobal % 32);

      // verificacao para caso o bit ser 1 ou 0
      if(bitEscrito == 1) {
        // uso de OR
        array[indPack] = array[indPack] | (1 << posPack);
      } else {
        // uso de AND
        array[indPack] = array[indPack] & ~(1 << posPack);
      } // fim do if-else
    } // fim do for
  } // fim do metodo

  /**************************************************************
  * Metodo: lerBits
  * Funcao: le uma sequencia especifica de bits de um array de inteiros
  * @param array | os bits recebidos
  * @param posInicialBit | posicao inicial do bit
  * @param numBits | numero de bits a ler
  * @return int | o valor inteiro formado pelos bits lidos
  * ********************************************************* */
  public static int lerBits(int[] array, int posInicialBit, int numBits) {
    int valorLido = 0; // o valor que sera retornado

    //loop para ler os bits
    for (int i = 0; i < numBits; i++) {
      int posicaoGlobal = posInicialBit + i; // calcula a posicao global do bit a ser lido
      int indiceDoPacote = posicaoGlobal / 32; // descobre em qual inteiro do array sera lido
      int posicaoNoPacote = 31 - (posicaoGlobal % 32); // calcula a posicao dentro do pacote

      int bitLido = (array[indiceDoPacote] >> posicaoNoPacote) & 1; // extrai o bit da posicao correta

      // se o bit lido for 1, o posiciona corretamente no valor de retorno
      if (bitLido == 1) {
        valorLido = valorLido | (1 << (numBits - 1 - i));
      } // fim do if
    } // fim do for
    return valorLido; // retorna o valor lido
  } // fim do metodo

  /**************************************************************
  * Metodo: lerBits
  * Funcao: calcula qual o total de bits real e trata os 0s inuteis
  * @param frame | os bits recebidos
  * @return int | numero de bits real
  * ********************************************************* */
  public static int tratarBits(int[] frame) {
    // verificacao de validade de frame
    if(frame == null || frame.length == 0) {
      return 0;
    } // fim do if

    int ultimoUm = -1;

    //loop para percorrer de tras pra frente procurando o ultimo 1
    for(int i = (frame.length * 32) - 1; i >= 0; i--) {
      //le o bit na posicao atual
      if(lerBits(frame, i, 1) == 1) {
        ultimoUm = i;
        break;
      } // fim do if
    } // fim do for

    //excecoes
    if(ultimoUm == -1) {
      //verificacao extra
      if(frame.length > 0) return 8;
      return 0;
    } // fim do if

    int byteTratamento = ultimoUm / 8;

    return (byteTratamento + 1) * 8;
  } // fim do metodo
} // fim da classe
