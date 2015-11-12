package mx.com.meda;

import org.apache.log4j.Logger;

import mx.com.meda.imp.SanbornsProcessor;
import mx.com.meda.imp.HitssProcessor;
import mx.com.meda.imp.HitssACProcessor;
import mx.com.meda.imp.OSTARProcessor;
import mx.com.meda.imp.IAVEProcessor;
import mx.com.meda.imp.ChedrauiProcessor;
import mx.com.meda.imp.InbursaProcessor;

public class ProcessorFactory {

	private static Logger log = Logger.getLogger(ProcessorFactory.class);

	public static Processor getProcessorInstance(Socio peer) {
		log.debug("Se generará un procesador del tipo: "+peer.getNombre());
		Processor procesador = null;
		switch(peer) {
			case HITSS : 
				procesador = new HitssProcessor();
				break;
			case HITSS_ACREDITACIONES : 
				procesador = new HitssACProcessor();
				break;
			case OSTAR :
				procesador = new OSTARProcessor();
				break;
			case IAVE :
				procesador = new IAVEProcessor();
				break;
			case CHEDRAUI :
				procesador = new ChedrauiProcessor();
				break;
			case SANBORNS :
				procesador = new SanbornsProcessor();
				break;
			case INBURSA :
				procesador = new InbursaProcessor();
				break;
		}
		return procesador;
	}

} 