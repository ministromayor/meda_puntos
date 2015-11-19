package mx.com.meda;

import org.apache.log4j.Logger;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

@Singleton(name = "JobHitssAcreditaciones")
public class JobHitssAcreditaciones {

	Logger log = Logger.getLogger(this.getClass());

	@Schedule(second="0", minute="*/1", hour="*", persistent=false)
	public void ejecutar() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HITSS_ACREDITACIONES);
		proc.procesarEntrada();
		proc.release();
	}

}
