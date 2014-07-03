package MineKNX;

public class InvalidException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1871689240734425336L;
	private String type;
	private String t;
	
	public InvalidException(String type, String t)
	{
		this.type=type;
		this.t=t;
	}
	
	@Override
	public String getMessage()
	{
		return "Invalid "+t+": '"+this.type+"'";
		
	}

}
