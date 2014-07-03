package MineKNX;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.material.Sign;
import org.bukkit.plugin.PluginManager;

import com.jcraft.jsch.JSchException;

public class KNXSign extends Sign
{
	private org.bukkit.block.Sign sign;
	private String ip;
	private String GAdress;
	private String GAdressStatus;
	private MineKNX.Main.type type;
	
	public KNXSign(org.bukkit.block.Sign sign, String ip, String GAdress, String GAdressStatus, MineKNX.Main.type type)
	{
		this.sign=sign;
		this.ip=ip;
		this.GAdress=GAdress;
		this.GAdressStatus=GAdressStatus;
		this.type=type;
	}
	
	private String getIp()
	{
		String ip;
		PluginManager pm= Bukkit.getPluginManager();
		if((ip=pm.getPlugin("MineKNX").getConfig().getString("ip."+this.ip)) != null)
		{
			return ip;
		}
		return this.ip;
	}
	
	public void runCommand(boolean signal)
	{
		String groupswriteBase="groupswrite ip:"+this.getIp()+" ";
		String groupsreadBase="groupreadresponse ip:"+this.getIp()+" ";
		try {
			this.type.Trigger(signal, this.GAdress, this.GAdressStatus, groupswriteBase, groupsreadBase);
		} catch (JSchException e) {
			Main.plugin.getLogger().warning(e.toString());
		} catch (IOException e) {
			Main.plugin.getLogger().warning(e.toString());
		}
	}

	public org.bukkit.block.Sign getSign() {
		
		return this.sign;
	}

}
