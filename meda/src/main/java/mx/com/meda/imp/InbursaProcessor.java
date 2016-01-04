package mx.com.meda.imp;

import org.apache.log4j.Logger;
import mx.com.meda.Processor;
import mx.com.meda.DataWrapper;
import mx.com.meda.SFTPClient;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.StringTokenizer;
import java.util.Properties;
import java.util.regex.Pattern;

import java.util.TimeZone;
import java.util.Date;
import java.util.List;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.JSchException;

import mx.com.meda.Socio;
import mx.com.meda.TipoDeArchivo;

public class InbursaProcessor extends AliadoProcessor implements Processor {

	private String file_name = null;
	private int lines = 0;
	private boolean empty_response = false;

	public InbursaProcessor() {
		super(Socio.INBURSA);
		log = Logger.getLogger(this.getClass());
	}

	public boolean procesarEntrada() {
		log.debug("Se comenzará la lectura del archivo de entrada.");
		boolean valid_trailer = false;
		boolean valid_header = false;
		boolean proc_header = false;
		try {
			if( cliente.conectar() )  {
				//int[] map = new int[]{2,10,7,9,16,7,10,14,15,15,15,15,16,3,12,3,31};
				int[] map = new int[]{2,10,7,9,16,7,10,14,15,15,15,15,31,3,31};
				file_name = cliente.lastAddedInFileName(buildInputFilename());
				if( file_name.length() > 0 ) {
					log.info("Se cargará el achivo: "+file_name);
					buildHeader(file_name);
					BufferedReader br = new BufferedReader(new InputStreamReader(cliente.readLastInFileLocalMode(file_name)));
					String linea = null;	

					while( (linea = br.readLine()) != null ) {
						log.debug(">>"+linea+"<");
						String[] values = new String[map.length+1];
						String[] tokens = null;
						values[0] = file_name;

						if( !proc_header && lines == 0 && in_header ) {
							log.debug("Se procesará el header.");
							valid_header = validarHeader(linea);
							proc_header = true;
						} else if(valid_header && !br.ready() && in_trailer) {
							log.debug("Se procesará el trailer.");
							valid_trailer = validarTrailer(linea);
						} else {
							if( linea.length() == in_campos ) {
								log.debug(">>"+linea);
								tokens = transformar(linea, map);
							} else {
								log.error("La linea ["+linea+"] mide "+linea.length()+" caracteres pero se esperaba que midiera "+in_campos);
							}
							if(tokens != null ) {
								log.debug("Se copiarán los tokens.");
								System.arraycopy(tokens, 0, values, 1, tokens.length);
								log.debug("Se cargarán los valores copiados desde los tokens en la base de datos.");
								dw.cargarLinea(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), values);
								tokens = null;
								lines++;
							}
						}
					}
					if( valid_trailer && valid_header && dw.procArchivoCarga(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), file_name) ) {
						cliente.backupInFile(file_name);	
						if( lines == 0 ) {
							empty_response = true;
						}
						this.procesarSalida();
					} else {
						log.error("No se procesará salida debido a que ocurrió un error durante el proceso de entrada.");
						dw.limpiarRegistrosFallidos(file_name);
					}
					br.close();
				} else {
					log.warn("No hay un archivo de entrada para procesar.");
				}
			}
		} catch( SftpException ex ) {
			log.error("No se puedo procesar la entrada.");
			log.warn(ex.getMessage());
		} finally {
			log.info("Se terminó el procesamiento.");
			cliente.desconectar();
			return true;
		}
	}

	public boolean procesarSalida() {
		log.debug("Se comenzará la generación del archivo de salida.");
		try {
			String out_filename = buildOutputFilename();
			List<Object[]> filas = null;
			if(!empty_response) {
				filas = dw.selArchivoSalida(TipoDeArchivo.RESPUESTA_ACRETIDACIONES.getId());
			}

			int registros = 0;
			long monto = 0L; 

			if(!empty_response && !filas.isEmpty()) {
				escribirRespuesta(buildHeader(file_name));
				for(Object[] arreglo : filas) {
					StringBuilder sb = new StringBuilder();
					for(int i = 0; i < arreglo.length; i++) {
						sb.append(arreglo[i]);
						// El campo 6 (indice 5) contiene los puntos que serán cargados.
						if(i == 5) {
							try {
								double ptos = Double.parseDouble((arreglo[i]).toString());
								log.debug("Se sumarán "+ptos+" al socio " + arreglo[3]);
								monto += ptos;
							} catch(NumberFormatException ex) {
								log.error("El campo IMPORTE DE PUNTOS no contiene un número.");
								log.debug(ex.getMessage());
							}
						}
					}
					registros++;
					String linea = sb.toString();
					log.debug("<<"+linea);
					escribirRespuesta(linea);
				}
				escribirRespuesta(buildTrailer(registros, monto));
				InputStream salida = recuperarRespuesta();
				cliente.uploadOutFile(salida, out_filename);
			} else if(empty_response) {
				log.info("Se creará una respuesta sin registros.");
				escribirRespuesta(buildHeader(file_name));
				escribirRespuesta(buildTrailer(registros, monto));
				InputStream salida = recuperarRespuesta();
				cliente.uploadOutFile(salida, out_filename);
			} else {
				log.warn("No se obtuvieron registros para generar un archivo de respuesta.");
			}
		} catch( Exception ex ) {
			ex.printStackTrace();
		} finally {
			return true;
		}
	}

	private String buildInputFilename() {
		String date_format = "yyyyMMdd";
		DateFormat df = new SimpleDateFormat(date_format);
		df.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
		String date = df.format(new Date());
		in_nombre = "CARS"+date+"?.???";
		return in_nombre;
	}

	private String buildOutputFilename() {
		String base_name = file_name.substring(0, file_name.lastIndexOf('.'));
		out_nombre = base_name+"_RESP.TXT";
		log.info("Se reportará el siguiente archivo: "+out_nombre);
		return out_nombre;
	}

	private boolean validarHeader(String header) {
		boolean flag = false;
		if(header.length() == in_h_campos) {
			int[] map = new int[]{2,5,14,16,14,16,2,135};
			String[] tokens = transformar(header, map);
			//Por el momento no hago nada con el header.
			for(int i = 0; i< tokens.length; i++) {
				log.debug("HEADER - Campo "+i+1+" Valor="+tokens[i]);
			}
			flag = true;
		} else {
			log.error("El header no tiene la longitud correcta.");
		}
		return flag;
	}

	private boolean validarTrailer(String trailer) {
		boolean flag = false;
		if( trailer.length() == in_t_campos ) {
			//int[] map = new int[]{2,6,8,8,8,8,8,8,14,129};
			int[] map = new int[]{2,6,8,8,175};
			String[] tokens = transformar(trailer, map);
			flag = validarTrailer(tokens, lines);
		}
		return flag;
	}

	private String[] transformar(String linea, int[] map) {
		int elements = map.length;
		log.debug("TRANSFORMACION - ELEMENTOS= "+elements);
		int offset = 0;
		String[] a_elements = new String[elements];
		for(int i = 0; i < elements; i++) {
			int f_len = map[i];
			a_elements[i] = linea.substring(offset, offset+f_len);
			offset += f_len;
			log.debug("TRANSFORMACION - INDICE= "+i+"\t VALOR= \'"+a_elements[i]+"\'");
		}
		log.debug("TRANFORMACION - FIN");
		return a_elements;
	}

	private boolean validarTrailer(String[] tokens, int registros) {
		boolean flag = false;
		if(in_trailer) {
			int lineas_recibidas = Integer.valueOf(tokens[1]).intValue();
			log.debug("El trailer indica: "+lineas_recibidas);
			if(lineas_recibidas == registros) {
				flag = true;
			} 
			log.debug("El proceso leyó: "+registros);
			log.info("El trailer es: "+ (flag ? "Valido" : "Erroneo") +" para "+registros+" registros.");
		} else {
			//este es un truco, se puede mejorar.
			flag = true;
		}
		return flag;
	}

	private String buildHeader(String file_name) {
		String date_format = "yyyyMMddHHmmss";
		DateFormat df = new SimpleDateFormat(date_format);
		df.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));

		String cmp_01 = "00";
		String cmp_02 = "CARSO";
		String cmp_03 = df.format(new Date());
		String cmp_04 = "                ";
		String cmp_05 = cmp_03;
		String cmp_06 = (file_name.substring(0, file_name.lastIndexOf('.')).toUpperCase())+"TXT";
		String cmp_07 = "00";
		String cmp_08 = "                                                                                                                                       ";

		StringBuilder sb = new StringBuilder();
		sb.append(cmp_01);
		sb.append(cmp_02);
		sb.append(cmp_03);
		sb.append(cmp_04);
		sb.append(cmp_05);
		sb.append(cmp_06);
		sb.append(cmp_07);
		sb.append(cmp_08);
		String header = sb.toString();
		log.debug("HEADER_CONSTRUIDO = \'"+header+"\'");

		return header;
	}

	private String buildTrailer(int registros, long ptos) {

		String cmp_01 = "99";
		String cmp_02 = String.format("%06d", new Integer(registros));
		String cmp_03 = String.format("%08d", new Long(ptos));
		String cmp_04 = cmp_03;
		String cmp_05 = "00000500";
		String cmp_06 = cmp_05;
		String cmp_07 = cmp_05;
		String cmp_08 = cmp_05;
		String cmp_09 = "00000000000000";
		String cmp_10 = "                                                                                                                                 ";

		StringBuilder sb = new StringBuilder();
		sb.append(cmp_01);
		sb.append(cmp_02);
		sb.append(cmp_03);
		sb.append(cmp_04);
		sb.append(cmp_05);
		sb.append(cmp_06);
		sb.append(cmp_07);
		sb.append(cmp_08);
		sb.append(cmp_09);
		sb.append(cmp_10);
		
		String trailer = sb.toString();
		log.debug("TRAILER_CONSTRUIDO = \'"+trailer+"\'");
		return trailer;
		
	}



}