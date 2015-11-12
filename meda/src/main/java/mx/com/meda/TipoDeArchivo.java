package mx.com.meda;

public enum TipoDeArchivo{
	RECIBE_TICKETS("Recibe carga de ticket", 1),
	RESPUESTA_TICKETS("Genera archivo de tickets", 2),
	RECIBE_ALTAS("Recibe carga de socios", 3),
	RESPUESTA_ALTAS("Genera archivo de socios", 4),
	RECIBE_ACREDITACIONES("Recibe carga de acreditaciones", 5),
	RESPUESTA_ACRETIDACIONES("Genera archivo de acreditacion", 6);

	private int id= 1;
	private String descripcion;

	TipoDeArchivo(String desc, int id) {
		this.id = id;
		this.descripcion = desc;
	}

	public int getId() {
		return this.id;
	}
	public String getDescripcion() {
		return this.descripcion;
	}

}