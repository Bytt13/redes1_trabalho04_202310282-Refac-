/*****************************************************************
* Autor..............: Lucas de Menezes Chaves
* Matricula........: 202310282
* Inicio...........: 16/09/2025
* Ultima alteracao.: 18/11/2025
* Nome.............: CamadaEnlaceDadosReceptora
* Funcao...........: Desenquadra o quadro, verifica erros e controla o fluxo de dados,
* enviando dados validos para a camada de aplicacao.
*************************************************************** */
package model;

import controller.TelaPrincipalController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import util.Erro;
import util.Auxiliares;

public class CamadaEnlaceDadosReceptora {

	private CamadaAplicacaoReceptora camadaAplicacaoReceptora;
	private TelaPrincipalController controller;

	// referencias as camadas irmas que o Host precisa, para saber onde interpretar
	// os ACKs
	private AplicacaoTransmissora aplicacaoTransmissoraIrma;
	private CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissoraIrma;

	// constantes do protocolo
	private final String ACK_PAYLOAD_STR = "ACK";

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptora
	* Funcao: construtor da classe
	* @param camadaAplicacaoReceptora | referencia para a camada de aplicacao receptora
	* @param controller | referencia para a interface grafica
	* @return void
	* ********************************************************* */
	public CamadaEnlaceDadosReceptora(CamadaAplicacaoReceptora camadaAplicacaoReceptora,
			TelaPrincipalController controller) {
		this.camadaAplicacaoReceptora = camadaAplicacaoReceptora;
		this.controller = controller;
	} // fim do construtor

	/**************************************************************
	* Metodo: setCamadaEnlaceTransmissoraIrma
	* Funcao: metodo que define a camda transmissora irma
	* @param camadaEnlaceDadosTransmissora | camadam de enlace irma da camda atual
	* @return void
	* ********************************************************* */
	public void setCamadaEnlaceTransmissoraIrma(CamadaEnlaceDadosTransmissora camadaEnlaceDadosTransmissora) {
		this.camadaEnlaceDadosTransmissoraIrma = camadaEnlaceDadosTransmissora;
	}// fim do setCamadaEnlaceTransmissoraIrma

	/**************************************************************
	* Metodo: setAplicacaoTransmissoraIrma
	* Funcao: metodo que define a aplicacao transmissora irma
	* @param aplicacaoTransmissora | a aplicacao transmissora que deve ser irma
	* @return void
	* ********************************************************* */
	public void setAplicacaoTransmissoraIrma(AplicacaoTransmissora aplicacaoTransmissora) {
		this.aplicacaoTransmissoraIrma = aplicacaoTransmissora;
	}// fim do setAplicacaoTransmissoraIrma

	/**************************************************************
	* Metodo: receber
	* Funcao: metodo responsavel por receber o quadro da camada fisica e processa-lo
	* @param quadro | quadro recebido da camada fisica com enquadramento e controle de erro incluidos
	* @return void
	* @throws Erro | trata os erros
	* ********************************************************* */
	public void receber(int[] quadro) throws Erro {

		int[] quadroVerificado;

		try {
			// tenta verificar o erro
			quadroVerificado = CamadaEnlaceDadosReceptoraControleDeErro(quadro);

		} catch (Erro e) {
			// Se uma excecao foi lancada, o quadro esta corrompido!

			System.out.println("Camada Enlace Receptora: ERRO DETECTADO. ");

			// informar o usuario, da deteccao de erros
			Platform.runLater(() -> {
				Alert alert = new Alert(AlertType.WARNING);
				alert.setTitle("Deteccao de Erro");
				alert.setHeaderText(e.getHeader());
				alert.setContentText(e.getContent());
				alert.show();
			});

			return; // sai do metodo sem processar o quadro
		} // fim try-catch

		// se chegou aqui o quadro esta valido

		int[] quadroDesenquadrado = CamadaEnlaceDadosReceptoraEnquadramento(quadroVerificado); // desenquadra o quadro

		CamadaEnlaceDadosReceptoraControleDeFluxo(quadroDesenquadrado); // controla o fluxo de dados

	} // fim do metodo receber

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraEnquadramento
	* Funcao: metodo que escolhe o tipo de desenquadramento a ser aplicado na mensagem
	* @param quadro | mensagem recebida da camada anterior com os bits enquadrados
	* @return int[] | o quadro ja desenquadrado
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraEnquadramento(int quadro[]) {
		int tipoDeEnquadramento = this.controller.enquadCodification();
		int[] quadroDesenquadrado = quadro;
		switch (tipoDeEnquadramento) {
			case 0: // contagem de caracteres
				quadroDesenquadrado = CamadaEnlaceDadosReceptoraEnquadramentoContagemDeCaracteres(quadro);
				break;
			case 1: // insercao de bytes
				quadroDesenquadrado = CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBytes(quadro);
				break;
			case 2: // insercao de bits
				quadroDesenquadrado = CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBits(quadro);
				break;
			case 3: // violacao da camada fisica
				quadroDesenquadrado = CamadaEnlaceDadosReceptoraEnquadramentoViolacaoDaCamadaFisica(quadro);
				break;
		}// fim do switch/case

		return quadroDesenquadrado; // retorna o quadro ja desenquadrado

	}// fim do metodo CamadaEnlaceDadosReceptoraEnquadramento

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeErro
	* Funcao: metodo que detecta e defin e qual verificacao de erro sera aplicada ao quadro
	* @param quadro | quadro recebido
	* @return int[] | o quadro verificado e com os bits de verificacao removidos
	* @throws Erro | trata os erros detectados
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraControleDeErro(int quadro[]) throws Erro {
		int tipoDeControleDeErro = this.controller.erroCodification();
		int[] quadroVerificado = null;
		switch (tipoDeControleDeErro) {
			case 0: // paridade par
				quadroVerificado = CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadePar(quadro);
				break;
			case 1: // paridade impar
				quadroVerificado = CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadeImpar(quadro);
				break;
			case 2: // CRC
				quadroVerificado = CamadaEnlaceDadosReceptoraControleDeErroCRC(quadro);
				break;
			case 3: // hamming
				quadroVerificado = CamadaEnlaceDadosReceptoraControleDeErroCodigoDeHamming(quadro);
				break;
		}// fim do switch/case
		return quadroVerificado;
	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeErro

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeFluxo
	* Funcao: metodo que sabe se o quadro eh de dados ou um ack, envia os acks e repassa os
	* dados para a proxima camada
	* @param quadro | quadro recebido que ta tendo o fluxo controlad
	* @return void
	* @throws Erro | trata os erros
	* ********************************************************* */
	public void CamadaEnlaceDadosReceptoraControleDeFluxo(int quadro[]) throws Erro {

		int tipoDeControleFluxo = this.controller.fluxoCodification();
		switch (tipoDeControleFluxo) {
			case 0: // janela deslizante de 1 bit
				CamadaEnlaceDadosReceptoraJanelaDeslizanteUmBit(quadro);
				break;
			case 1: // janela deslizante go-back-n
				CamadaEnlaceDadosReceptoraJanelaDeslizanteGoBackN(quadro);
				break;
			case 2: // janela deslizante com retransmissão seletiva
				CamadaEnlaceDadosReceptoraJanelaDeslizanteComRetransmissaoSeletiva(quadro);
				break;
			default:
				break;
		} // fim do swich case

	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeFluxo

	/**************************************************************
	* Metodo: fluxoStopWait
	* Funcao: Armazena a logica do Stop and Wait para o lado receptor.
	* @param quadro | quadro a ser processado
	* @return void
	* @throws Erro | para tratamento de erro no fluxo
	* ********************************************************* */
	public void fluxoStopWait(int[] quadro) throws Erro {
		// converte mensagem quadro para String para verificar se eh um ACK ou nao, caso
		// nao seja um ACK entao eh uma mensagem que ta sendo enviada pra B
		String payload = Auxiliares.packToString(quadro);

		if (payload.equals(ACK_PAYLOAD_STR)) {
			// eh um ACK
			System.out.println("CAMADA ENLACE RECEPTORA - ACK recebido");

			// notifica a camada transmissora irma, para ela saber que tem um ACK chegando
			// para ela
			if (this.camadaEnlaceDadosTransmissoraIrma != null) {
				// verificacao de seguranca
				this.camadaEnlaceDadosTransmissoraIrma.receberAck();
			} // fim if

		} else {
			// se nao eh ACK eh quadro
			System.out.println("CAMADA ENLACE RECEPTORA: Dados validos recebidos passando para aplicacao");

			if (this.camadaAplicacaoReceptora != null) {
				// envia o quadro pra proxima camada
				this.camadaAplicacaoReceptora.receber(quadro);
			} // fim do if

			// como dados foram recebidos e estao validos, envia o ack para a transmissora
			System.out.println("CAMADA ENLACE RECEPTORA: Enviando ACK de volta...");
			if (this.aplicacaoTransmissoraIrma != null) {
				// Usa a pilha de transmissão irma, para enviar o ACK pelo mesmo caminho
				this.aplicacaoTransmissoraIrma.transmitir(ACK_PAYLOAD_STR);
			} // fimif

		} // fim if/else
	}

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraEnquadramentoContagemDeCaracteres
	* Funcao: metodo para desenquadrar o quadro utilizando o metodo de contagem de
	* caracteres
	* @param quadro | quadro recebido com os bits enquadrados
	* @return int[] | o quadro ja desenquadrado
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraEnquadramentoContagemDeCaracteres(int quadro[]) {

		// usa o tamanho real do sinal para evitar ler lixo
		int maximoDeBitsNoQuadro = Auxiliares.tratarBits(quadro);
		if (maximoDeBitsNoQuadro == 0)
			return new int[0];

		// usa um buffer grande e depois apara para o tamanho exato
		int[] bufferTemporario = new int[quadro.length];
		int bitEscritaGlobal = 0;
		int bitLeituraGlobal = 0;

		while (bitLeituraGlobal < maximoDeBitsNoQuadro) { // loop enquanto tiver bits pra ler
			// garante que ha espaco para ler um cabecalho
			if (bitLeituraGlobal + 8 > maximoDeBitsNoQuadro)
				break;

			// le o cabecalho de 8 bits
			int contagem = Auxiliares.lerBits(quadro, bitLeituraGlobal, 8);
			bitLeituraGlobal += 8;

			if (contagem == 0)
				break;

			// le a carga util
			int bitsCargaUtil = (contagem - 1) * 8; // calcula quantos bits serao de carga util

			// garante que a carga util nao ultrapassa o final do quadro
			if (bitLeituraGlobal + bitsCargaUtil > maximoDeBitsNoQuadro)
				break;

			int cargaUtil = Auxiliares.lerBits(quadro, bitLeituraGlobal, bitsCargaUtil);
			bitLeituraGlobal += bitsCargaUtil;

			// escreve a carga util no buffer de saida
			Auxiliares.escreverBits(bufferTemporario, bitEscritaGlobal, cargaUtil, bitsCargaUtil);
			bitEscritaGlobal += bitsCargaUtil;
		} // fim while

		// Cria o array final com o tamanho EXATO dos dados extraidos.
		int[] quadroDesenquadrado = new int[(bitEscritaGlobal + 31) / 32];
		for (int i = 0; i < bitEscritaGlobal; i++) {
			int bit = Auxiliares.lerBits(bufferTemporario, i, 1);
			Auxiliares.escreverBits(quadroDesenquadrado, i, bit, 1);
		} // fim for

		return quadroDesenquadrado;
	}// fim do metodo CamadaEnlaceDadosReceptoraEnquadramentoContagemDeCaracteres

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBytes
	* Funcao: metodo para desenquadrar o quadro utilizando o metodo de insercao de bytes
	* @param quadro | quadro recebido com os bits enquadrados
	* @return int[] | o quadro ja desenquadrado
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBytes(int quadro[]) {

		final int FLAG = 0b01111110;
		final int SCAPE = 0b01111101;

		// 1. Determina o total de bytes a serem lidos
		int totalBitsQuadro = Auxiliares.tratarBits(quadro);
		int contadorBytesRecebidos = totalBitsQuadro / 8;
		
		if (contadorBytesRecebidos == 0) return new int[0];

		// 2. Calcula quantos bytes de carga útil existirão após o desenquadramento
		int contadorBytesCargaUtil = 0;
		for (int i = 0; i < contadorBytesRecebidos; i++) {
			int byteAtual = Auxiliares.lerBits(quadro, i * 8, 8);

			if (byteAtual == FLAG) {
				continue;
			}

			if (byteAtual == SCAPE) {
				i++;
				if (i < contadorBytesRecebidos) {
					contadorBytesCargaUtil++;
				}
			} else {
				contadorBytesCargaUtil++;
			}
		}

		// 3. Cria o array final com o tamanho exato que foi calculado
		int tamanhoQuadroDesenquadrado = (contadorBytesCargaUtil * 8 + 31) / 32;
		int[] quadroDesenquadrado = new int[tamanhoQuadroDesenquadrado];

		int indiceBitDestino = 0;

		// 4. Percorre os bytes recebidos novamente para extrair e escrever a carga útil
		for (int i = 0; i < contadorBytesRecebidos; i++) {
			int byteAtual = Auxiliares.lerBits(quadro, i * 8, 8);

			if (byteAtual == FLAG) {
				continue;
			}

			if (byteAtual == SCAPE) {
				i++;
				if (i >= contadorBytesRecebidos) break;

				int byteDeDados = Auxiliares.lerBits(quadro, i * 8, 8);

				Auxiliares.escreverBits(quadroDesenquadrado, indiceBitDestino, byteDeDados, 8);
				indiceBitDestino += 8;
			} else {
				Auxiliares.escreverBits(quadroDesenquadrado, indiceBitDestino, byteAtual, 8);
				indiceBitDestino += 8;
			}
		}

		return quadroDesenquadrado;
	}// fim do metodo CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBytes

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBits
	* Funcao: metodo para desenquadrar o quadro utilizando a insercao de bits (bit unstuffing)
	* @param quadro | quadro recebido enquadrado
	* @return int[] | o quadro ja desenquadrado
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBits(int quadro[]) {

		final int FLAG = 0b01111110;

		int tamanhoMaximoEstimado = Auxiliares.tratarBits(quadro);
		if (tamanhoMaximoEstimado == 0) return new int[0];
		
		int[] bufferTemporario = new int[quadro.length];
		int indiceBitDestino = 0;
		int contadorBitsUm = 0;
		boolean inicioQuadro = false;
		
		int i = 0;
		
		while (i < tamanhoMaximoEstimado) {
			// 1. Dtecta inicio de quadro e descarta flag
			if (!inicioQuadro) {
				if (i + 8 <= tamanhoMaximoEstimado) {
					int possivelFlag = Auxiliares.lerBits(quadro, i, 8);
					if (possivelFlag == FLAG) {
						inicioQuadro = true;
						i += 8;
						contadorBitsUm = 0;
						continue;
					}
				}
				i++;
				continue;
			}
			
			// 2. Comenca a processar os dados
			
			// A. Verifica se os próximos 8 bits formam uma flag (final de quadro)
			if (i + 8 <= tamanhoMaximoEstimado) {
				int possivelFlag = Auxiliares.lerBits(quadro, i, 8);
				if (possivelFlag == FLAG) {
					i += 8;
					contadorBitsUm = 0;
					continue;
				}
			}
			
			// B. Leitura de um bit do quadro de entrada
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			i++;

			// C. Descarte do bit de stuffing
			if (contadorBitsUm == 5) {
				if (bitAtual == 0) {
					contadorBitsUm = 0;
					continue; // descarta o bit '0' e continua o loop
				}
			}

			// D. Escreve o bit de dado no buffer temporario
			Auxiliares.escreverBits(bufferTemporario, indiceBitDestino, bitAtual, 1);
			indiceBitDestino++;

			// E. Atualiza o contador de bits '1'
			if (bitAtual == 1) {
				contadorBitsUm++;
			} else {
				contadorBitsUm = 0;
			}
		} // fim while

		// cria array final com tamanho exato
		int tamanhoArrayFinal = (indiceBitDestino + 31) / 32;
		int[] quadroDesenquadrado = new int[tamanhoArrayFinal];

		// Copia os bits do buffer temporário para o array final
		for (i = 0; i < indiceBitDestino; i++) {
			int bit = Auxiliares.lerBits(bufferTemporario, i, 1);
			Auxiliares.escreverBits(quadroDesenquadrado, i, bit, 1);
		}

		return quadroDesenquadrado;
	} // fim do metodo CamadaEnlaceDadosReceptoraEnquadramentoInsercaoDeBits

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraEnquadramentoViolacaoDaCamadaFisica
	* Funcao: Passa somente o quadro pra proxima camada, pois a responsabilidade de
	* desenquadramento foi violada e colocada pra camada fisica
	* @param quadro | o quadro ja desenquadrado
	* @return int[] | o mesmo quadro
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraEnquadramentoViolacaoDaCamadaFisica(int quadro[]) {

		return quadro;
	} // fim CamadaEnlaceDadosReceptoraEnquadramentoViolacaoDaCamadaFisica

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadePar
	* Funcao: metodo para verificar erros no quadro utilizando o metodo de bit de paridade par
	* @param quadro | quadro recebido
	* @return int[] | quadro verificado, e removido os bits de controle
	* @throws Erro | trata os erros
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadePar(int quadro[])
			throws Erro {

		int totalBitsRecebidos = Auxiliares.tratarBits(quadro);

		if (totalBitsRecebidos == 0) {
			throw new Erro("QUADRO COM 0 BITS", "quadro nao possui bits!");
		}

		int totalBitsReaisVerificar = totalBitsRecebidos - 7; // remove os bits de padding

		// conta o numero de bits 1
		int contadorUns = 0;
		for (int i = 0; i < totalBitsReaisVerificar; i++) {
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			if (bitAtual == 1) {
				contadorUns++;
			}
		} // fim for

		if (contadorUns % 2 != 0) {
			// se o numero de uns for impar, entao houve erro
			throw new Erro("ERRO BIT DE PARIDADE PAR!!",
					"O quadro recebido possui um numero impares de bits '1', a verificacao esperava um numero par. \n QUADRO DESCARTADO!!");
		}

		// nao teve erro
		int totalBitsSemControle = totalBitsReaisVerificar - 1; // remove o bit de paridade

		// Se o quadro so tinha o bit de paridade (ou estava vazio), retorna vazio
		if (totalBitsSemControle <= 0) {
			return new int[0];
		}

		int tamanhoArrayFinal = (totalBitsSemControle + 31) / 32;
		int[] quadroVerificado = new int[tamanhoArrayFinal];

		// escreve no quadro sem o controle os bits uteis
		for (int i = 0; i < totalBitsSemControle; i++) {
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			Auxiliares.escreverBits(quadroVerificado, i, bitAtual, 1);
		} // fim for

		return quadroVerificado;
	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadePar

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadeImpar
	* Funcao: metodo para verificar erros no quadro utilizando o metodo de bit de paridade impar
	* @param quadro | quadro recebido
	* @return int[] | quadro verificado, e removido os bits de controle
	* @throws Erro | trata os erros
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadeImpar(int quadro[])
			throws Erro {

		int totalBitsRecebidos = Auxiliares.tratarBits(quadro);

		if (totalBitsRecebidos == 0) {
			throw new Erro("QUADRO COM 0 BITS", "quadro nao possui bits!");
		}

		int totalBitsReaisVerificar = totalBitsRecebidos - 7; // remove os bits de padding

		// conta o numero de bits 1
		int contadorUns = 0;
		for (int i = 0; i < totalBitsReaisVerificar; i++) {
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			if (bitAtual == 1) {
				contadorUns++;
			}
		} // fim for

		if (contadorUns % 2 == 0) {
			// se o numero de uns for par, entao houve erro
			throw new Erro("ERRO BIT DE PARIDADE IMPAR!!",
					"O quadro recebido possui um numero par de bits '1', a verificacao esperava um numero impar. \n QUADRO DESCARTADO!!");
		}

		// nao teve erro
		int totalBitsSemControle = totalBitsReaisVerificar - 1; // remove o bit de paridade

		// Se o quadro so tinha o bit de paridade (ou estava vazio), retorna vazio
		if (totalBitsSemControle <= 0) {
			return new int[0];
		}

		int tamanhoArrayFinal = (totalBitsSemControle + 31) / 32;
		int[] quadroVerificado = new int[tamanhoArrayFinal];

		// escreve no quadro sem o controle os bits uteis
		for (int i = 0; i < totalBitsSemControle; i++) {
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			Auxiliares.escreverBits(quadroVerificado, i, bitAtual, 1);
		} // fim for

		return quadroVerificado;
	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeErroBitDeParidadeImpar

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeErroCRC
	* Funcao: metodo que verifica o erro dos quadros com o controle CRC
	* @param quadro | quadro possivelmente com erro
	* @return int[] | quadro verificado
	* @throws Erro | trata os erros
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraControleDeErroCRC(int quadro[]) throws Erro {

		int totalBitsRecebidos = Auxiliares.tratarBits(quadro);

		final int POLINOMIO_GERADOR = 0x04C11DB7;
		final int VALOR_INICIAL = 0xFFFFFFFF;
		final int VALOR_FINAL_XOR = 0xFFFFFFFF;

		int registradorCRC = VALOR_INICIAL;

		if (totalBitsRecebidos < 32) {
			throw new Erro("QUADRO INVALIDO", "menos de 32 bits no quadro!");
		}

		int totalBitsReaisVerificar = totalBitsRecebidos - 32;

		// No CRC-32 (IEEE 802), o cálculo é feito em todos os bits (dados + CRC).
		// Se o resto for zero, o quadro é aceito.
		for (int i = 0; i < totalBitsRecebidos; i++) {
			int bitAtual = Auxiliares.lerBits(quadro, i, 1);
			int bitMaisSignificativo = (registradorCRC >> 31) & 1;
			int xorBit = bitMaisSignificativo ^ bitAtual;

			registradorCRC = registradorCRC << 1;

			if (xorBit == 1) {
				registradorCRC = registradorCRC ^ POLINOMIO_GERADOR;
			}
		}
		
		// Aplica o XOR final (com 0xFFFFFFFF) no resto
		registradorCRC = registradorCRC ^ VALOR_FINAL_XOR;

		// Verifica se o resultado é zero (ou se o cálculo intermediário deu zero)
		if (registradorCRC != 0) {
			String msgErro = String.format(
					"Erro de CRC-32!\n\nResto Final do Calculo: 0x%X\n\nO quadro foi descartado.", registradorCRC);

			throw new Erro("FALHA DE CRC (CHECKSUM)", msgErro);
		}

		// se nao foi corrompido entao remove o CRC
		int novoTamanhoArray = (totalBitsReaisVerificar + 31) / 32;
		int[] quadroVerificado = new int[novoTamanhoArray];

		for (int i = 0; i < totalBitsReaisVerificar; i++) {
			int bit = Auxiliares.lerBits(quadro, i, 1);
			Auxiliares.escreverBits(quadroVerificado, i, bit, 1);
		} // fim for

		return quadroVerificado;
	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeErroCRC

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraControleDeErroCodigoDeHamming
	* Funcao: metodo que verifica e corrige os quadros com o controle de erro hamming
	* @param quadro | quadro possivelmente com erro
	* @return int[] | quadro corrigido
	* ********************************************************* */
	public int[] CamadaEnlaceDadosReceptoraControleDeErroCodigoDeHamming(int quadro[]) {

		int totalBitsReal = Auxiliares.tratarBits(quadro);

		if (totalBitsReal == 0) {
			return new int[0];
		}

		// descobrir a quantidade de bits de paridade da mensagem
		int quantBitsParidade = 0;
		while ((1 << quantBitsParidade) < (totalBitsReal + 1)) {
			quantBitsParidade++;
		}
		
		if (quantBitsParidade < 0) return new int[0];
		
		int posicaoErro = 0;

		// calcula e descobre se tem e onde tem o erro
		for (int i = 0; i < quantBitsParidade; i++) {

			int posBitParidade = 1 << i;
			int contadorUns = 0;

			// percorre todos os bits cobertos pelo bit de paridade do momento
			for (int bit = 1; bit <= totalBitsReal; bit++) {
				if ((bit & posBitParidade) != 0) {
					if (Auxiliares.lerBits(quadro, bit - 1, 1) == 1) {
						contadorUns++;
					}
				}
			} // fim for bit

			// Se a contagem total for IMPAR, a paridade PAR falhou.
			if (contadorUns % 2 != 0) {
				posicaoErro += posBitParidade;
			}

		} // fim for i

		// corrigir possivel erro
		if (posicaoErro > 0 && posicaoErro <= totalBitsReal) {
			System.out.println("HAMMING RX: Erro detectado. Corrigindo bit...");

			// inverte o bit na posicao do erro
			int bitAtual = Auxiliares.lerBits(quadro, posicaoErro - 1, 1);
			Auxiliares.escreverBits(quadro, posicaoErro - 1, 1 - bitAtual, 1);
		}

		// extrai os dados corrigidos
		int totalBitsFinal = totalBitsReal - quantBitsParidade;
		int tamanhoQuadroVerificado = (totalBitsFinal + 31) / 32;
		int[] quadroVerificado = new int[tamanhoQuadroVerificado];

		int indiceEscrita = 0;
		for (int i = 1; i <= totalBitsReal; i++) {
			// se i NAO for potencia de 2 ele eh carga util entao copia
			if (!((i & (i - 1)) == 0)) {
				int bitCargaUtil = Auxiliares.lerBits(quadro, i - 1, 1);
				Auxiliares.escreverBits(quadroVerificado, indiceEscrita, bitCargaUtil, 1);
				indiceEscrita++;
				if (indiceEscrita >= totalBitsFinal) {
					break;
				}
			}
		} // fim for

		return quadroVerificado;
	}// fim do metodo CamadaEnlaceDadosReceptoraControleDeErroCodigoDeHamming

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraJanelaDeslizanteUmBit
	* Funcao: Implementacao do controle de fluxo Janela Deslizante de 1 bit (Stop and Wait)
	* @param quadro | quadro a ser processado
	* @return void
	* @throws Erro | para tratamento de erro no fluxo
	* ********************************************************* */
	public void CamadaEnlaceDadosReceptoraJanelaDeslizanteUmBit(int quadro[]) throws Erro {

		fluxoStopWait(quadro);
	}// fim do metodo CamadaEnlaceDadosReceptoraJanelaDeslizanteUmBit

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraJanelaDeslizanteGoBackN
	* Funcao: (Implementacao Pendente)
	* @param quadro | quadro a ser processado
	* @return void
	* ********************************************************* */
	public void CamadaEnlaceDadosReceptoraJanelaDeslizanteGoBackN(int quadro[]) {

	} // fim do metodo CamadaEnlaceDadosReceptoraJanelaDeslizanteGoBackN

	/**************************************************************
	* Metodo: CamadaEnlaceDadosReceptoraJanelaDeslizanteComRetransmissaoSeletiva
	* Funcao: (Implementacao Pendente)
	* @param quadro | quadro a ser processado
	* @return void
	* ********************************************************* */
	public void CamadaEnlaceDadosReceptoraJanelaDeslizanteComRetransmissaoSeletiva(int quadro[]) {

	}// fim do metodo
} // fim da classe CamadaEnlaceDadosReceptora