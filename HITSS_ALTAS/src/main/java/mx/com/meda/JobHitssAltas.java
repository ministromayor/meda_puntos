package mx.com.meda;

import org.apache.log4j.Logger;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

@Singleton(name = "JobHitssAltas")
public class JobHitssAltas {

	Logger log = Logger.getLogger(this.getClass());

	@Schedule(second="0", minute="*/10", hour="*", persistent=false)
	public void entrada() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HITSS);
		proc.procesarEntrada();
		proc.release();
		proc = null;
	}

	@Schedule(second="20", minute="*/10", hour="*", persistent=false)
	public void wa() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HITSS);
		proc.workarround();
		proc.release();
		proc = null;
	}

	@Schedule(second="40", minute="*/10", hour="*", persistent=false)
	public void salida() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HITSS);
		proc.procesarSalida();
		proc.release();
		proc = null;
	}

}
