package ru.l2gw.gameserver.handler;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.SocialAction;
import ru.l2gw.gameserver.tables.SkillTable;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * @author rage
 * @date 11.08.2010 10:58:26
 */
public class RequestActionHandler
{
	private static Log _log = LogFactory.getLog(RequestActionHandler.class);
	private static RequestActionHandler _instance;
	private static final TIntObjectHashMap<RequestAction> _actions = new TIntObjectHashMap<>();

	private RequestActionHandler()
	{
		_log.info("RequestActionHandler: initializing...");
		load();
		_log.info("RequestActionHandler: loaded " + _actions.size() + " actions.");
	}

	public static RequestActionHandler getInstance()
	{
		if(_instance == null)
			_instance = new RequestActionHandler();
		return _instance;
	}

	public RequestAction getActionById(int actionId)
	{
		return _actions.get(actionId);
	}

	private void load()
	{
		try
		{
			File file = new File(Config.ACTIONS_CONFIG);

			if(!file.exists())
			{
				if(Config.DEBUG)
					_log.info("The " + Config.ACTIONS_CONFIG + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("action".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();

							int actionId;
							RequestAction.Type type;
							try
							{
								actionId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							}
							catch(Exception e)
							{
								_log.warn("RequestActionHandler: no `id` attribute for action!");
								continue;
							}

							try
							{
								type = RequestAction.Type.valueOf(attrs.getNamedItem("type").getNodeValue().toLowerCase());
							}
							catch(Exception e)
							{
								_log.warn("RequestActionHandler: unknown `type` attribute for action id: " + actionId + " type: " + (attrs.getNamedItem("type") != null ? attrs.getNamedItem("type").getNodeValue() : "null"));
								continue;
							}

							boolean transform = attrs.getNamedItem("transform") == null || Boolean.parseBoolean(attrs.getNamedItem("transform").getNodeValue());
							RequestAction ra = new RequestAction(actionId, type, transform);

							if(attrs.getNamedItem("social") != null)
								try
								{
									ra.setSocialType(SocialAction.SocialType.valueOf(attrs.getNamedItem("social").getNodeValue().toUpperCase()));
								}
								catch(Exception e)
								{
									_log.warn("RequestActionHandler: unknown `social` attribute for action id: " + actionId + " social: " + attrs.getNamedItem("social").getNodeValue());
									continue;
								}

							if(attrs.getNamedItem("skill_id") != null)
								try
								{
									ra.setSkillId(Integer.parseInt(attrs.getNamedItem("skill_id").getNodeValue()));
								}
								catch(Exception e)
								{
									_log.warn("RequestActionHandler: can't parse `skill_id` attribute for action id: " + actionId + " skill_id: " + attrs.getNamedItem("skill_id").getNodeValue());
									continue;
								}

							if(attrs.getNamedItem("skills") != null && attrs.getNamedItem("levels") != null)
							{
								String[] skills = attrs.getNamedItem("skills").getNodeValue().split(";");
								String[] levels = attrs.getNamedItem("levels").getNodeValue().split(";");

								if(skills.length != levels.length)
								{
									_log.warn("RequestActionHandler: skills count do not match with levels count for action id: " + actionId);
									continue;
								}

								for(int i = 0; i < skills.length; i++)
								{
									try
									{
										String skillInfo = skills[i];
										if(!skillInfo.isEmpty())
										{
											L2Skill skill = SkillTable.getInstance().getInfo(Integer.parseInt(skillInfo.split("-")[0]), Integer.parseInt(skillInfo.split("-")[1]));
											ra.addSkillByLevel(skill, Integer.parseInt(levels[i]));
										}
									}
									catch(Exception e)
									{
										_log.warn("RequestActionHandler: can't parse `skills` and levels attribute for action id: " + actionId + " skills: " + attrs.getNamedItem("skills").getNodeValue() + " levels: " + attrs.getNamedItem("skills").getNodeValue() + " " + e);
									}
								}
							}

							if(_actions.containsKey(actionId))
								_log.warn("RequestActionHandler: action with id: " + actionId + " already loaded.");

							_actions.put(actionId, ra);
						}
		}
		catch(Exception e)
		{
			System.err.println("Error while loading actions data.");
			e.printStackTrace();
		}
	}
}
