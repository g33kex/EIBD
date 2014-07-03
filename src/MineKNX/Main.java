package MineKNX;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Main extends JavaPlugin implements Listener
{
	public String pluginName = "MineKNX";
	
	private ArrayList<KNXSign> signsList = new ArrayList<KNXSign>();
	
	static private String user;
	static private String password;
	static private String server;
	static private int port;
	static private String path;
	public static Main plugin;

	
	@Override
	public void onEnable()
	{
		plugin = this;
		PluginManager pm= Bukkit.getPluginManager();
		getConfig().options().copyDefaults(true);
		saveConfig();
		loadSigns();
		
		Main.user=getConfig().getString("user");
		Main.password=getConfig().getString("password");
		Main.path=getConfig().getString("path");
		Main.server=getConfig().getString("server");
		Main.port=getConfig().getInt("port");
		
		pm.registerEvents(this, this);

		getLogger().info(pluginName+" enable");
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event)
	{
		loadSigns();
	}
	
	private void loadSigns()
	{
		for(World world : Bukkit.getWorlds())
		{
			for(Chunk chunk : world.getLoadedChunks())
			{
				for(BlockState state : chunk.getTileEntities())
				{
					if(state.getType()==Material.WALL_SIGN || state.getType()==Material.SIGN_POST)
					{
						Sign s = (Sign) state;
						if(s.getLine(0).startsWith((ChatColor.YELLOW+"[KNX:")) && s.getLine(0).endsWith("]"))
						try {
							addSign(s, s.getLines());
						} catch (InvalidException e) {
						}
					}
				}
			}
		}
		
	}

	@EventHandler
	public void onBlockRedstone(BlockRedstoneEvent event)
	{
		if(event.getBlock().getType()==Material.WALL_SIGN || event.getBlock().getType()==Material.SIGN_POST)
		{
			Sign sign = (Sign)event.getBlock().getState();

			if(event.getNewCurrent()==0)
			{
				handleSign(sign, false);	
			}
			else if(event.getNewCurrent()>0)
			{
				handleSign(sign, true);
			}


		}
		//Bukkit.broadcastMessage("Redstone update :"+event.getBlock().getType());
	}
	
	private void handleSign(Sign sign, boolean state) 
	{
		for(KNXSign s : this.signsList)
		{
			Sign s1 = s.getSign();
			if(sign.getX()==s1.getX() && sign.getY()==s1.getY() && sign.getZ()==s1.getZ())
			{
				s.runCommand(state);
			}
			
		}
		
	}

	@EventHandler
	public void onSignChange(SignChangeEvent event)
	{
		if(event.getLine(0).startsWith("[KNX:") && event.getLine(0).endsWith("]"))
		{
			//s.setLine(0, ChatColor.RED+event.getLine(0));
		try {
			addSign((Sign) event.getBlock().getState(), event.getLines());
			event.setLine(0, ChatColor.YELLOW+event.getLine(0));
		} catch (InvalidException e) {
			String[] message = e.getMessage().split(":");
			event.setLine(0, ChatColor.RED+"[ERROR]");
			event.setLine(1, "");
			event.setLine(2, message[0]+":");
			event.setLine(3, message[1]);
		}
		}
	}
	
	private void addSign(Sign sign, String[] lines) throws InvalidException 
	{
			String KT = lines[0].substring(1, lines[0].length()-1);
			String[] kt = KT.split(":");
			if(kt.length<2)
			{
				throw new InvalidException("formatting", KT);
			}
			String tstring=kt[1];
			String ip=lines[1];
			String AGroup=lines[2];
			String AGroupStatus=lines[3];
			type t = type.getEnumByName(tstring);
			if(t==null)
			{
				throw new InvalidException(lines[3], "type");
			}
			if(ip=="")
			{
				throw new InvalidException("", "ip");
			}
			if(AGroup=="")
			{
				throw new InvalidException("", "adress");
			}
			if(AGroupStatus=="")
			{
				throw new InvalidException("", "statusAdress");
			}
			//Bukkit.broadcastMessage("Ip:"+ip+"  Adress:"+AGroup+"  Type:"+t.getName());
			if(!listContainsXYZ(sign.getX(), sign.getY(), sign.getZ()))
			{
				this.signsList.add(new KNXSign(sign, ip, AGroup, AGroupStatus,t));
			}
	}
	
	private boolean listContainsXYZ(int x, int y, int z)
	{
		for(KNXSign sign : this.signsList)
		{
			Sign s = sign.getSign();
			if(s.getX()==x && s.getY()==y && s.getZ()==z)
			{
				return true;
			}
		}
		return false;
	}

	public enum type
	{
		SWITCH("switch", "Switch from on to off depending of the redstone state. Send one packet for one call.") {
			@Override
			public void Trigger(boolean current,String gAdress, String gAdressStatus, String groupswriteBase, String groupsreadBase) throws JSchException, IOException {
					runSSH(groupswriteBase+gAdress+" "+(current ? 1 : 0));
					runSSH(groupswriteBase+gAdressStatus+" "+(current ? 1 : 0));
				
			}
		},
		/*SWITCHUP("switch*", "Switch from on to off depending of the redstone state. Continusly sending packets") {
			@Override
			public void Trigger(boolean current) {
				// TODO Auto-generated method stub
				
			}
		},*/
		TELE("tele", "Inverse the current state. ")
		{
			@Override
			public void Trigger(boolean current, String gAdress, String gAdressStatus,String groupswriteBase, String groupsreadBase) throws JSchException, IOException
			{
				if(current)
				{
					String result = runSSH(groupsreadBase+gAdressStatus);
					Bukkit.broadcastMessage(result);
				}
			}
		};
		/*FLIPFLOPUP("flipflop*", "Inverse the current state, corresponding to the state packet of the BUS") {
			@Override
			public void Trigger(boolean current) {
				// TODO Auto-generated method stub
				
			}
		};*/
		
		
		private String name;
		private String description;
		private static type[] types = {SWITCH, TELE};
		
		public abstract void Trigger(boolean current, String gAdress,String gAdressStatus, String groupswriteBase, String groupsreadBase) throws JSchException, IOException;
		private type(String name, String description)
		{
			this.name=name;
			this.description=description;
		}
		
		public String getName()
		{
			return this.name;
		}
		
		public String getDescription()
		{
			return this.description;
		}
		
		public static type getEnumByName(String name)
		{
			for(type t : types)
			{
				if(t.getName().equalsIgnoreCase(name))
				{
					return t;
				}
			}
			return null;
		}
	}

	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event)
	{
		Material block = event.getBlock().getType();
		Player player = event.getPlayer();
		
		if(block==Material.TNT)
		{
			player.getWorld().spawnEntity(event.getBlock().getLocation(), EntityType.PRIMED_TNT);
			event.getBlock().breakNaturally();
			player.sendMessage(ChatColor.RED+"Primmed !");
		}
	}
	
	
	@Override
	public void onDisable()
	{
		getLogger().info(pluginName+" disable");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(command.getName().equalsIgnoreCase("mineknx"))
		{
			sender.sendMessage("MineKNX is loaded");
		}
		getLogger().warning(sender.getName()+" did the command "+command.getName());
		return true;
	}

	public static String runSSH(String command) throws JSchException, IOException
	{
		//try{
		String r = "";
			JSch jsch = new JSch();
			
			Session session = jsch.getSession(user, server, port);
			
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(password);
			session.connect();
			Channel channel = session.openChannel("exec");
			((ChannelExec) channel).setCommand(path+command);
			
			plugin.getLogger().info("§cRunning... §1|§r §6command:§r"+command+"  §6server:§r"+session.getHost());
			((ChannelExec) channel).setErrStream(System.err);
			
			InputStream in = channel.getInputStream();
			channel.connect();
			
			byte[] tmp = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    r+=new String(tmp, 0, i);
                }
                if (channel.isClosed()) {
                	plugin.getLogger().info("Channel closed !");
                    break;
                }
            }
    	channel.disconnect();
    	session.disconnect();
		//}catch(Exception e){Bukkit.getPluginManager().getPlugin("MineKNX").getLogger().warning(e.toString());}
		return r;
		
	}
	
}
