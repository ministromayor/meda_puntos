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

	public InbursaProcessor() {
		super(Socio.INBURSA);
		log = Logger.getLogger(this.getClass());
	}

	public boolean procesarEntrada() {
		log.debug("Se comenzará la lectura del archivo de entrada.");
		int lines = 0;
		boolean valid_trailer = false;
		boolean valid_header = false;
		try {
			if( cliente.conectar() )  {
				int[] map = new int[]{2,10,7,9,16,7,10,14,15,15,15,15,31,3,31};
				String file_name = cliente.lastAddedInFileName(buildInputFilename());
				log.info("Se cargará el achivo: "+file_name);
				BufferedReader br = new BufferedReader(new InputStreamReader(cliente.readLastInFile(file_name)));
				String linea = null;	
				while( (linea = br.readLine()) != null ) {
					String[] values = new String[map.length+1];
					String[] tokens = null;
					values[0] = file_name;
					if(lines == 0 && in_header ) {
						log.debug("Se procesará el header.");
						valid_header = validarHeader(linea);
					} else if(lines != 0 && !br.ready() && in_trailer ) {
						log.debug("Se procesará el trailer.");
						valid_trailer = validarTrailer(linea);
					} else {
						if( linea.length() == in_campos ) {
							log.info(">>"+linea);
							tokens = transformar(linea, map);
						} else {
							log.error("La linea ["+linea+"] mide "+linea.length()+" caracteres pero se esperaba que midiera "+in_campos);
						}
					}
					if(tokens != null ) {
						System.arraycopy(tokens, 0, values, 1, in_campos);
						dw.cargarLinea(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), values);
						lines++;
					}
				}
				if( valid_trailer && valid_header && dw.procArchivoCarga(TipoDeArchivo.RECIBE_ACREDITACIONES.getId(), file_name) ) {
					this.procesarSalida();
				} else {
					log.error("No se procesará salida debido a que ocurrió un error durante el proceso de entrada.");
				}
				br.close();
				cliente.desconectar();
			}
		} catch( SftpException ex ) {
			log.error("No se puedo procesar la entrada.");
			log.warn(ex.getMessage());
		} finally {
			return true;
		}
	}

	public boolean procesarSalida() {
		//implementar código para la generación del header y el trailer.
		log.debug("Se comenzará la generación del archivo de salida.");
		try {
			String out_filename = buildOutputFilename();
			List<Object[]> filas = dw.selArchivoSalida(TipoDeArchivo.RESPUESTA_ACRETIDACIONES.getId());
			if(!filas.isEmpty()) {
				for(Object[] arreglo : filas) {
					StringBuilder sb = new StringBuilder();
					for(int i = 0; i < (out_campos-1); i++) {
						sb.append(arreglo[i]);
					}
					sb.append(arreglo[out_campos-1]);
					String linea = sb.toString();
					log.info("<<"+linea);
					escribirRespuesta(linea);
				}
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
		in_nombre = "CARS"+date+".txt";
		return in_nombre;
	}

	private String buildOutputFilename() {
		String date_format = "ddMMyyyy";
		DateFormat df = new SimpleDateFormat(date_format);
		df.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
		String date = df.format(new Date());
		out_nombre = "CARS"+date+"_RESP.txt";
		log.info("Se reportará el siguiente archivo: "+out_nombre);
		return out_nombre;
	}

	private boolean validarHeader(String header) {
		boolean flag = false;
		if(header.length() == in_h_campos) {
			int[] map = new int[]{2,5,14,30,16,137};
			String[] tokens = transformar(header, map);
			//Por el momento no hago nada con el header.
			flag = true;
		} else {
			log.error("El header no tiene la longitud correcta.");
		}
		return flag;
	}

	private boolean validarTrailer(String trailer) {
		boolean flag = false;
		if( trailer.length() == in_t_campos ) {
			int[] map = new int[]{2,6,8,8,175};
			String[] tokens = transformar(trailer, map);
			flag = validarTrailer(tokens, 2);
		}
		return flag;
	}

	private String[] transformar(String linea, int[] map) {
		int elements = map.length;
		int offset = 0;
		String[] a_elements = new String[elements];
		for(int i = 0; i < elements; i++) {
			int f_len = map[i];
			a_elements[i] = linea.substring(offset, f_len);
			offset += f_len;
			log.debug("Se extrajo la cadena "+a_elements[i]+" en el indice: "+i+" de la linea.");
		}
		return a_elements;
	}

	private boolean validarTrailer(String[] tokens, int registros) {
		boolean flag = false;
		if(in_trailer) {
			int lineas_recibidas = Integer.valueOf(tokens[1]).intValue();
			if((tokens.length == in_t_campos) && (lineas_recibidas == registros)) {
				flag = true;
			} 
			log.info("El trailer es: "+ (flag ? "Valido" : "Erroneo") +" para "+registros+" registros.");
		} else {
			//este es un truco, se puede mejorar.
			flag = true;
		}
		return flag;
	}

}