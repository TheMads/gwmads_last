package ru.l2gw.gameserver.model.gmaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.limits.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * @author: rage
 * @date: 10.03.12 11:10
 */
public class AdminTemplateManager
{
	private static final Log log = LogFactory.getLog(AdminTemplateManager.class);

	private static final HashMap<String, AdminTemplate> templates = new HashMap<>();
	private static final HashMap<String, String> gmList = new HashMap<>();
	private static AdminTemplate defaultTemplate;
	
	public static void reload()
	{
		templates.clear();
		gmList.clear();
		load();
	}

	public static AdminTemplate getAdminTemplate(L2Player player)
	{
		if(player == null)
			return null;

		return getAdminTemplate(player.getName());
	}
	
	public static AdminTemplate getAdminTemplate(String name)
	{
		if(gmList.containsKey(name))
			return templates.get(gmList.get(name));

		return defaultTemplate;
	}
	
	public static boolean checkBoolean(String property, L2Player player)
	{
		AdminTemplate template = getAdminTemplate(player);
		return template != null && template.checkBoolean(property, player);
	}
	
	public static boolean checkCommand(String command, L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		AdminTemplate template = getAdminTemplate(player);
		return template != null && template.checkLimits(command, player, target, arg1, arg2, arg3);
	}

	public static boolean checkCommandAllow(String command, L2Player player)
	{
		AdminTemplate template = getAdminTemplate(player);
		return template != null && template.checkCommand(command);
	}

	private static void load()
	{
		File dir = new File("config/gmaccess/templates");
		if(!dir.exists())
		{
			log.warn("GM template directory: " + dir.getAbsolutePath() + " not exists");
			return;
		}

		for(File f : dir.listFiles())
		{
			if(f.getName().endsWith(".xml"))
			{
				try
				{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setValidating(false);
					factory.setIgnoringComments(true);
					Document doc = factory.newDocumentBuilder().parse(f);
					HashMap<String, GArray<String>> commandLists = new HashMap<>();

					for(Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
					{
						if("templates".equals(list.getNodeName()))
						{
							for(Node n = list.getFirstChild(); n != null; n = n.getNextSibling())
							{
								if("command-list".equals(n.getNodeName()))
								{
									String listName = n.getAttributes().getNamedItem("name").getNodeValue();
									if(n.getFirstChild() != null)
									{
										StringTokenizer data = new StringTokenizer(n.getFirstChild().getNodeValue());
										GArray<String> commands = new GArray<>();
										while(data.hasMoreTokens())
										{
											commands.add(data.nextToken());
										}
										commandLists.put(listName, commands);
									}
								}
								else if("template".equals(n.getNodeName()))
								{
									String name = n.getAttributes().getNamedItem("name").getNodeValue();
									String allowCommands = n.getAttributes().getNamedItem("allow-command-list") != null ? n.getAttributes().getNamedItem("allow-command-list").getNodeValue() : "";
									String denyCommands = n.getAttributes().getNamedItem("deny-command-list") != null ? n.getAttributes().getNamedItem("deny-command-list").getNodeValue() : "";
									boolean def = n.getAttributes().getNamedItem("default") != null && Boolean.parseBoolean(n.getAttributes().getNamedItem("default").getNodeValue());

									AdminTemplate template = new AdminTemplate(name);
									GArray<String> allowCommandList = null;
									GArray<String> denyCommandList = null;

									if(allowCommands.contains(","))
									{
										String[] commands = ArrayUtils.toStringArray(allowCommands);
										allowCommandList = new GArray<>(commands.length);
										Collections.addAll(allowCommandList, commands);
									}
									else if(commandLists.containsKey(allowCommands))
										allowCommandList = commandLists.get(allowCommands);

									if(denyCommands.contains(","))
									{
										String[] commands = ArrayUtils.toStringArray(denyCommands);
										denyCommandList = new GArray<>(commands.length);
										Collections.addAll(denyCommandList, commands);
									}
									else if(commandLists.containsKey(denyCommands))
										denyCommandList = commandLists.get(denyCommands);

									template.setAllowCommands(allowCommandList);
									template.setDenyCommands(denyCommandList);

									for(Node limitNode = n.getFirstChild(); limitNode != null; limitNode = limitNode.getNextSibling())
									{
										if("limit".equals(limitNode.getNodeName()))
										{
											String commands = limitNode.getAttributes().getNamedItem("command-group") != null ? limitNode.getAttributes().getNamedItem("command-group").getNodeValue() : null;
											String commandMatch = limitNode.getAttributes().getNamedItem("command-match") != null ? limitNode.getAttributes().getNamedItem("command-match").getNodeValue() : "";
											GArray<String> commandsLimitList = null;

											if(commands != null)
											{
												if(commands.contains(","))
												{
													String[] commandsArray = ArrayUtils.toStringArray(commands);
													commandsLimitList = new GArray<>(commandsArray.length);
													Collections.addAll(commandsLimitList, commandsArray);
												}
												else if(commandLists.containsKey(commands))
													commandsLimitList = commandLists.get(commands);
											}

											AdminCommandLimit limit = new AdminCommandLimit(commandMatch, commandsLimitList);
											limit.addCommandLimits(parseLimit(limitNode));
											template.addLimit(limit);
										}
										else if("property".equals(limitNode.getNodeName()))
										{
											String pName = limitNode.getAttributes().getNamedItem("name").getNodeValue();
											String value = limitNode.getAttributes().getNamedItem("value").getNodeValue();

							                template.setProperty(pName, value);
											for(Node lp = limitNode.getFirstChild(); lp != null; lp = lp.getNextSibling())
											{
												if("limit".equals(lp.getNodeName()))
												{
													AdminCommandLimit limit = new AdminCommandLimit(pName, null);
													limit.addCommandLimits(parseLimit(lp));
													template.addPropertyLimit(pName, limit);
												}
											}
										}
									}

									if(def)
									{
										if(defaultTemplate != null)
											log.warn("AdminTemplateManager: default template already set to: " + defaultTemplate);
										else
										{
											defaultTemplate = template;
											log.info("AdminTemplateManager: default template " + name);
										}
									}

									templates.put(name, template);
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					log.warn("Can't load gm template file: " + f.getAbsolutePath() + " " + e);
					e.printStackTrace();
				}
			}
		}

		dir = new File("config/gmaccess");
		if(!dir.exists())
		{
			log.warn("GM directory: " + dir.getAbsolutePath() + " not exists");
			return;
		}

		for(File f : dir.listFiles())
		{
			if(f.getName().endsWith(".xml"))
			{
				try
				{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setValidating(false);
					factory.setIgnoringComments(true);
					Document doc = factory.newDocumentBuilder().parse(f);

					for(Node list = doc.getFirstChild(); list != null; list = list.getNextSibling())
					{
						if("list".equals(list.getNodeName()))
						{
							for(Node p = list.getFirstChild(); p != null; p = p.getNextSibling())
							{
								if("player".equals(p.getNodeName()))
								{
									String name = p.getAttributes().getNamedItem("name").getNodeValue();
									String template = p.getAttributes().getNamedItem("template").getNodeValue();
									if(name.isEmpty() || template.isEmpty())
										continue;

									if(templates.containsKey(template))
										gmList.put(name, template);
									else
										log.warn("AdminTemplateManager: " + template + " not found for player: " + name);
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					log.warn("Can't load gm list file: " + f.getAbsolutePath() + " " + e);
					e.printStackTrace();
				}
			}
		}

		log.info("AdminTemplateManager: loaded " + templates.size() + " templates.");
		log.info("AdminTemplateManager: loaded " + gmList.size() + " GM's.");
	}

	private static GArray<IAdminLimit> parseLimit(Node limitNode)
	{
		GArray<IAdminLimit> limits = new GArray<>();
		for(Node limit = limitNode.getFirstChild(); limit != null; limit = limit.getNextSibling())
		{
			if(limit.getNodeType() == Node.ELEMENT_NODE)
			{
				if("range".equals(limit.getNodeName()))
				{
					limits.add(new RangeLimit(limit.getAttributes().getNamedItem("points").getNodeValue(), limit.getAttributes().getNamedItem("check-target") == null || Boolean.parseBoolean(limit.getAttributes().getNamedItem("check-target").getNodeValue())));
				}
				else if("time".equals(limit.getNodeName()))
				{
					String start = limit.getAttributes().getNamedItem("start").getNodeValue();
					String end = limit.getAttributes().getNamedItem("end").getNodeValue();
					String days = limit.getAttributes().getNamedItem("days") != null ? limit.getAttributes().getNamedItem("days").getNodeValue() : "";
					limits.add(new TimeLimit(start, end, days));
				}
				else if("target".equals(limit.getNodeName()))
				{
					limits.add(new TargetLimit(limit.getAttributes().getNamedItem("type").getNodeValue()));
				}
				else if("access".equals(limit.getNodeName()))
				{
					limits.add(new AccessLimit(limit.getAttributes().getNamedItem("min-level").getNodeValue()));
				}
				else if("arg".equals(limit.getNodeName()))
				{
					NamedNodeMap attr = limit.getAttributes();
					int num = Integer.parseInt(attr.getNamedItem("num").getNodeValue());
					if(attr.getNamedItem("min") != null)
					{
						limits.add(new ArgMinLimit(num, attr.getNamedItem("min").getNodeValue()));
					}
					if(attr.getNamedItem("max") != null)
					{
						limits.add(new ArgMaxLimit(num, attr.getNamedItem("max").getNodeValue()));
					}
					if(attr.getNamedItem("list") != null)
					{
						if(attr.getNamedItem("type") != null && "string".equalsIgnoreCase(attr.getNamedItem("type").getNodeValue()))
							limits.add(new ArgStringListLimit(num, attr.getNamedItem("list").getNodeValue()));
						else
							limits.add(new ArgIntListLimit(num, attr.getNamedItem("list").getNodeValue()));
					}
				}
				else if("and".equals(limit.getNodeName()))
				{
					limits.add(new LogicAndLimit(parseLimit(limit)));
				}
				else if("or".equals(limit.getNodeName()))
				{
					limits.add(new LogicOrLimit(parseLimit(limit)));
				}
				else if("not".equals(limit.getNodeName()))
				{
					limits.add(new LogicNotLimit(new LogicAndLimit(parseLimit(limit))));
				}
				else
				{
					log.warn("AdminTemplateManager: unsupported limit: " + limit.getNodeName());
				}
			}
		}

		return limits;
	}
}