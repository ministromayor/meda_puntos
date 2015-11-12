package mx.com.meda;

import org.apache.log4j.Logger;
import javax.ejb.Schedule;
import javax.ejb.Singleton;

@Singleton(name = "JobHitssAltas")
public class JobHitssAltas {

	Logger log = Logger.getLogger(this.getClass());

	@Schedule(second="0", minute="*/10", hour="*", persistent=false)
	public void ejecutar() {
		Processor proc = ProcessorFactory.getProcessorInstance(Socio.HITSS);
		proc.procesarEntrada();
		proc.release();
	}

}
