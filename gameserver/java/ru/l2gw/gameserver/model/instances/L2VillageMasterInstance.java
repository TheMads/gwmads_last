package ru.l2gw.gameserver.model.instances;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.MaintenanceManager;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.L2Clan.SubPledge;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.base.PlayerClass;
import ru.l2gw.gameserver.model.base.Race;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.siege.SiegeDatabase;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.CharTemplateTable;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.ClassMasterTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.HashMap;
import java.util.List;

public final class L2VillageMasterInstance extends L2NpcInstance
{
	private static Log _log = LogFactory.getLog(L2VillageMasterInstance.class.getName());

	/**
	 * @param template
	 */
	public L2VillageMasterInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		String type = "";

		if(getAIParams() != null)
			type = getAIParams().getString("class_master_type", "");

		List<ClassId> npcClasses = ClassMasterTable.getInstance().getClassListForType(type);

		if(command.startsWith("create_clan") && command.length() > 12)
		{
			String val = command.substring(12);
			createClan(player, val);
		}
		else if(command.startsWith("create_academy") && command.length() > 15)
		{
			String sub = command.substring(15, command.length());
			createSubPledge(player, sub, L2Clan.SUBUNIT_ACADEMY, 5, "");
		}
		else if(command.startsWith("create_royal") && command.length() > 15)
		{
			String[] sub = command.substring(13, command.length()).split(" ", 2);
			if(sub.length == 2)
				createSubPledge(player, sub[1], L2Clan.SUBUNIT_ROYAL1, 6, sub[0]);
		}
		else if(command.startsWith("create_knight") && command.length() > 16)
		{
			String[] sub = command.substring(14, command.length()).split(" ", 2);
			if(sub.length == 2)
				createSubPledge(player, sub[1], L2Clan.SUBUNIT_KNIGHT1, 7, sub[0]);
		}
		else if(command.startsWith("assign_subpl_leader") && command.length() > 22)
		{
			String[] sub = command.substring(20, command.length()).split(" ", 2);
			if(sub.length == 2)
				assignSubPledgeLeader(player, sub[1], sub[0]);
		}
		else if(command.startsWith("assign_new_clan_leader") && command.length() > 23)
		{
			String val = command.substring(23);
			setLeader(player, val);
			if(Config.DEBUG)
				_log.debug("Clan " + player.getClan() + " assign new clan leader: " + val + ".");
		}
		if(command.startsWith("create_ally") && command.length() > 12)
		{
			String val = command.substring(12);
			createAlly(player, val);
		}
		else if(command.startsWith("dissolve_ally"))
			dissolveAlly(player);
		else if(command.startsWith("dissolve_clan"))
			dissolveClan(player);
		else if(command.startsWith("increase_clan_level"))
			levelUpClan(player);
		else if(command.startsWith("learn_clan_skills"))
			showClanSkillList(player);
		else if(command.equalsIgnoreCase("Subclass"))
		{
			NpcHtmlMessage html;

			if(getVillageMasterRace() == Race.kamael && player.getRace() != Race.kamael)
				html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/subclass-kamael.htm", 1);
			else if(player.getRace() == Race.kamael && getVillageMasterRace() != Race.kamael)
				html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/subclass-no-kamael.htm", 1);
			else if(type.equalsIgnoreCase("dwarfBlacksmith"))
				html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/subclass-change-only.htm", 1);
			else
				html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/subclass.htm", 1);

			player.sendPacket(html);
		}
		else if(command.startsWith("Subclass "))
		{
			NpcHtmlMessage html;

			HashMap<Short, L2SubClass> playerClassList = player.getSubClasses();

			short classId = 0;
			short newClassId = 0;
			int intVal = 0;

			try
			{
				for(String id : command.substring(9, command.length()).split(" "))
				{
					if(intVal == 0)
					{
						intVal = Integer.parseInt(id);
						continue;
					}
					if(classId > 0)
					{
						newClassId = Short.parseShort(id);
						continue;
					}
					classId = Short.parseShort(id);
				}
			}
			catch(Exception NumberFormatException)
			{
			}

			switch(intVal)
			{
				case 1: // Возвращает список сабов, которые можно взять (см case 4)
					html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add.htm", 1);

					if(type.isEmpty())
					{
						html.replace("%classlist%", "no type defined for this class master, please make screenshot and report to administration.");
						player.sendPacket(html);
						return;
					}

					if(type.equalsIgnoreCase("dwarfBlacksmith"))
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/change-dwarf.htm", 1);
						player.sendPacket(html);
						return;
					}
					//subsAvailable = getAvailableSubClasses(player);
					StringBuffer buff = new StringBuffer();
					//subsAvailable
					if(npcClasses != null && !npcClasses.isEmpty())
					{
						for(ClassId subClass : npcClasses)
						{
							if(subClass == ClassId.overlord)
								continue;
							else if(subClass == ClassId.inspector && playerClassList.size() < 3)
								continue;
							else if(subClass == ClassId.maleSoulbreaker && player.getSex() == 1 || subClass == ClassId.femaleSoulbreaker && player.getSex() == 0)
								continue;

							buff.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Subclass 4 ").append(subClass.ordinal()).append("\" msg=\"1268;").append(CharTemplateTable.getClassNameById(subClass.ordinal())).append("\">").append(CharTemplateTable.getClassNameById(subClass.ordinal())).append("</a><br>");
						}
					}

					html.replace("%classlist%", buff.toString());
					player.sendPacket(html);
					break;
				case 2: // Установка уже взятого саба (см case 5)

					if(playerClassList.size() < 2)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/change-no-sub.htm", 2);
						player.sendPacket(html);
						return;
					}

					List<Short> availForChange = new FastList<Short>();

					for(Short subId : playerClassList.keySet())
					{
						try
						{
							if(npcClasses.contains(ClassId.values()[subId]) || npcClasses.contains(ClassId.values()[subId].getParent(player.getSex())))
								availForChange.add(subId);
						}
						catch(NullPointerException e)
						{
							_log.info("Warning NPE in subclass change: " + player + " classId: " + subId);
						}
					}

					if(availForChange.size() > 0)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/change.htm", 2);
						buff = new StringBuffer();

						for(Short subId : availForChange)
							buff.append("<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " + subId + "\"><ClassID>" + subId + "</ClassID></a><br>");

						html.replace("%classlist%", buff.toString());
					}
					else
						html = new NpcHtmlMessage(player, this, type.equalsIgnoreCase("dwarfBlacksmith") ? "data/html/villagemaster/subclass/change-dwarf.htm" : "data/html/villagemaster/subclass/change-err.htm", 2);

					player.sendPacket(html);

					break;
				case 3: // Отмена сабкласса - список имеющихся (см case 6)
					html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-cancel-and-add.htm", 3);

					String[] playerSubs = new String[3];
					for(L2SubClass sub : playerClassList.values())
					{
						if(sub.getSlot() > 0)
							playerSubs[sub.getSlot() - 1] = "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 6 " + sub.getClassId() + "\"><ClassID>" + sub.getClassId() + "</ClassID></a>";
					}

					for(int i = 1; i < 4; i++)
						html.replace("%subclass" + i + "%", playerSubs[i - 1] == null ? "" : playerSubs[i - 1]);

					player.sendPacket(html);
					break;
				case 4: // Добавление сабкласса - обработка выбора из case 1

					if(!checkSubChangeCond(player))
						return;

					if(player.getRace() == Race.elf && ClassId.values()[classId].getRace() == Race.darkelf || player.getRace() == Race.darkelf && ClassId.values()[classId].getRace() == Race.elf)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-elves-err.htm", 4);
						player.sendPacket(html);
						return;
					}

					PlayerClass pc = PlayerClass.values()[classId];
					String sametype = null;
					boolean exist = false;

					for(Short subId : playerClassList.keySet())
					{
						ClassId subClass = ClassId.values()[subId];
						if(subClass.getLevel() == 4)
							subClass = subClass.getParent(player.getSex());

						if(subClass.getId() == classId)
						{
							exist = true;
							break;
						}

						if(PlayerClass.subclassSetMap.containsKey(pc) && PlayerClass.subclassSetMap.get(pc).contains(PlayerClass.values()[subClass.getId()]))
						{
							sametype = "";
							for(PlayerClass pl : PlayerClass.subclassSetMap.get(pc))
								sametype += CharTemplateTable.getClassNameById(pl.ordinal()) + " (" + CharTemplateTable.getClassNameById(ClassId.values()[pl.ordinal()].getFirstChild().getId()) + "), ";
							break;
						}
					}

					if(exist || sametype != null)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-err-same.htm", 4);
						if(sametype != null)
							html.replace("%sametype%", sametype);
						else
							html.replace("%sametype%", CharTemplateTable.getClassNameById(classId) + " (" + CharTemplateTable.getClassNameById(ClassId.values()[classId].getFirstChild().getId()) + "), ");
						player.sendPacket(html);
						return;
					}

					boolean allowAddition = true;

					for(L2SubClass sub : playerClassList.values())
					{
						if(sub.getLevel() < Config.ALT_GAME_LEVEL_TO_GET_SUBCLASS)
						{
							allowAddition = false;
							break;
						}
					}

					if(playerClassList.size() >= 4)
						allowAddition = false;

					/*
					 * Если требуется квест - проверка прохождения Mimir's Elixir (Path to Subclass)
					 * Для камаэлей квест 236_SeedsOfChaos
					 * Если саб первый, то проверить начилие предмета, если не первый, то даём сабкласс.
					 * Если сабов нету, то проверяем наличие предмета.
					 */
					if(!Config.ALT_GAME_SUBCLASS_WITHOUT_QUESTS && allowAddition && !playerClassList.isEmpty() && playerClassList.size() < 2 + Config.ALT_GAME_SUB_ADD && !player.isNoble())
					{
						QuestState qs = player.getQuestState("_234_FatesWhisper");
						allowAddition = qs != null && qs.isCompleted();
						if(allowAddition)
						{
							if(player.getRace() == Race.kamael)
							{
								qs = player.getQuestState("_236_SeedsOfChaos");
								allowAddition = qs != null && qs.isCompleted();
							}
							else
							{
								qs = player.getQuestState("_235_MimirsElixir");
								allowAddition = qs != null && qs.isCompleted();
							}
						}
					}

					if(allowAddition)
					{
						ClassId currentClass = ClassId.values()[player.getActiveClass()];
						allowAddition = currentClass.getLevel() >= 3;
					}

					if(allowAddition)
					{
						player.stopAllEffects();
						if(!player.addSubClass(classId))
						{
							player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.SubclassCouldNotBeAdded", player));
							return;
						}
						player.sendPacket(new SystemMessage(SystemMessage.THE_NEW_SUB_CLASS_HAS_BEEN_ADDED));
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-success.htm", 4);
					}
					else
						html = new NpcHtmlMessage(player, this, player.getRace() == Race.kamael ? "data/html/villagemaster/subclass/add-kamael-err-cond.htm" : "data/html/villagemaster/subclass/add-err-cond.htm", 4);

					player.sendPacket(html);
					break;

				case 5: // Смена саба на другой из уже взятых - обработка выбора из case 2

					if(!checkSubChangeCond(player))
						return;

					if(player.getActiveClass() == classId)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/change-same.htm", 5);
						player.sendPacket(html);
						return;
					}

					player.stopAllEffects();
					player.setActiveSubClass(classId, true);

					player.sendPacket(new SystemMessage(SystemMessage.THE_TRANSFER_OF_SUB_CLASS_HAS_BEEN_COMPLETED)); // Transfer
					// completed.
					break;
				case 6: // Отмена сабкласса - обработка выбора из case 3
					html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-cancel-add2.htm", 6);
					String[] slots = {"first", "second", "third"};

					html.replace("%subclassslot%", slots[playerClassList.get(classId).getSlot() - 1]);

					buff = new StringBuffer();

					if(npcClasses != null && !npcClasses.isEmpty())
					{
						for(ClassId subClass : npcClasses)
						{
							if(subClass == ClassId.overlord)
								continue;
							else if(subClass == ClassId.inspector && playerClassList.size() < 3)
								continue;
							else if(subClass == ClassId.maleSoulbreaker && player.getSex() == 1 || subClass == ClassId.femaleSoulbreaker && player.getSex() == 0)
								continue;

							buff.append("<a action=\"bypass -h npc_").append(getObjectId()).append("_Subclass 7 ").append(classId).append(" ").append(subClass.ordinal()).append("\" msg=\"1445;").append("\">").append(CharTemplateTable.getClassNameById(subClass.ordinal())).append("</a><br>");
						}
					}

					html.replace("%classlist%", buff.toString());
					player.sendPacket(html);
					break;
				case 7: // Отмена сабкласса - обработка выбора из case 6
					if(!checkSubChangeCond(player))
						return;

					if(player.getRace() == Race.elf && ClassId.values()[newClassId].getRace() == Race.darkelf || player.getRace() == Race.darkelf && ClassId.values()[newClassId].getRace() == Race.elf)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-elves-err.htm", 7);
						player.sendPacket(html);
						return;
					}

					pc = PlayerClass.values()[newClassId];
					sametype = null;
					exist = false;

					for(Short subId : playerClassList.keySet())
					{
						if(subId == classId)
							continue;

						ClassId subClass = ClassId.values()[subId];
						if(subClass.getLevel() == 4)
							subClass = subClass.getParent(player.getSex());

						if(subClass.getId() == newClassId)
						{
							exist = true;
							break;
						}

						if(PlayerClass.subclassSetMap.containsKey(pc) && PlayerClass.subclassSetMap.get(pc).contains(PlayerClass.values()[subClass.getId()]))
						{
							sametype = "";
							for(PlayerClass pl : PlayerClass.subclassSetMap.get(pc))
								sametype += CharTemplateTable.getClassNameById(pl.ordinal()) + " (" + CharTemplateTable.getClassNameById(ClassId.values()[pl.ordinal()].getFirstChild().getId()) + "), ";
							break;
						}
					}

					if(exist || sametype != null)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-err-same.htm", 7);
						if(sametype != null)
							html.replace("%sametype%", sametype);
						else
							html.replace("%sametype%", CharTemplateTable.getClassNameById(newClassId) + " (" + CharTemplateTable.getClassNameById(ClassId.values()[newClassId].getFirstChild().getId()) + "), ");
						player.sendPacket(html);
						return;
					}

					player.stopAllEffects();
					ClassId oldClass = player.getClassId();
					if(player.modifySubClass(classId, newClassId))
					{
						L2ItemInstance pomander;
						int itemId = 0;
						switch(oldClass)
						{
							case cardinal:
								itemId = L2Item.ITEM_ID_POMANDER_CARDINAL;
								break;
							case evaSaint:
								itemId = L2Item.ITEM_ID_POMANDER_EVAS_SAINT;
								break;
							case shillienSaint:
								itemId = L2Item.ITEM_ID_POMANDER_SHILIEN_SAINT;
								break;
						}

						if(itemId > 0)
						{
							while((pomander = player.getWarehouse().getItemByItemId(itemId)) != null)
								player.getWarehouse().destroyItem("SubDeletion", pomander, player, this);

							while((pomander = player.getInventory().getItemByItemId(itemId)) != null)
								player.destroyItem("SubDeletion", pomander.getObjectId(), pomander.getCount(), this, true);
						}

						player.sendPacket(new SystemMessage(SystemMessage.THE_NEW_SUB_CLASS_HAS_BEEN_ADDED)); // Subclass added.
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/add-success.htm", 7);
						player.sendPacket(html);
					}
					else
						player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.SubclassCouldNotBeAdded", player));
					break;
			}
		}
		else if(command.startsWith("skillcert "))
		{
			if(!player.isSubClassActive())
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-nosub.htm", 8);
				player.sendPacket(html);
				return;
			}

			if(player.getLevel() < 65)
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-nolvl1.htm", 8);
				player.sendPacket(html);
				return;
			}

			ClassId cId = ClassId.values()[player.getActiveClass()];

			if(cId.getLevel() == 4)
				cId = cId.getParent(player.getSex());

			if(npcClasses == null || !npcClasses.contains(ClassId.values()[cId.getId()]))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-wrongsub.htm", 8);
				player.sendPacket(html);
				return;
			}

			int val;
			try
			{
				val = Integer.parseInt(command.substring(10));
			}
			catch(Exception e)
			{
				_log.info("Invalid bypass: " + command);
				return;
			}

			NpcHtmlMessage html;
			L2SubClass sc = player.getSubClasses().get(player.getActiveClass());
			switch(val)
			{
				case 65:
				case 70:
				case 75:
				case 80:
					if(player.getLevel() < val)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-nolvl2.htm", 9);
						html.replace("%level%", Integer.toString(val));
						player.sendPacket(html);
						return;
					}

					if(player.getVar("cert-" + sc.getSlot() + "-" + val) != null)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-already.htm", 9);
						player.sendPacket(html);
						return;
					}

					html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-lvl" + val + ".htm", 9);
					html.replace("%class_id%", Short.toString(player.getActiveClass()));
					html.replace("%level%", Integer.toString(val));
					player.sendPacket(html);

					break;
				case 6501:
				case 7001:
				case 7501:
				case 7502:
				case 8001:

					String varName = "cert-" + sc.getSlot() + "-" + Integer.toString(val).substring(0, 2);

					if(player.getVar(varName) != null)
					{
						html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-already.htm", 9);
						player.sendPacket(html);
						return;
					}

					int certItemId = 10280;
					if(val == 7501) // Certificate - Master Ability
						certItemId = 10612;
					else if(val == 7502) // Certificate - class specific
					{
						certItemId = sc.getCertificateItemId();
						if(certItemId == 0)
						{
							_log.info("Warning: no cerificate item for " + sc.getClassId() + " " + player);
							return;
						}
					}
					else if(val == 8001) // Divine Tramsform
					{
						certItemId = sc.getCertificateTransform();
						if(certItemId == 0)
						{
							_log.info("Warning: no transform cerificate item for " + sc.getClassId() + " " + player);
							return;
						}
					}

					player.setVar(varName, "true");
					player.addItem("Certification", certItemId, 1, this, true);

					player.sendPacket(new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-get.htm", 9));
					break;
				default:
					html = new NpcHtmlMessage(player, this, "data/html/villagemaster/subclass/skillcert-ok.htm", 9);
					player.sendPacket(html);
			}
		}
		else
			super.onBypassFeedback(player, command);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/villagemaster/" + pom + ".htm";
	}

	// Private stuff

	public void createClan(L2Player player, String clanName)
	{
		if(Config.DEBUG)
			_log.debug(player.getObjectId() + "(" + player.getName() + ") requested clan creation from " + getObjectId() + "(" + getName() + ")");
		if(player.getLevel() < Config.MinLevelToCreatePledge)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ARE_NOT_QUALIFIED_TO_CREATE_A_CLAN));
			return;
		}

		if(player.getClanId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN));
			return;
		}

		if(!player.canCreateClan())
		{
			// you can't create a new clan within 10 days
			player.sendPacket(new SystemMessage(SystemMessage.YOU_MUST_WAIT_10_DAYS_BEFORE_CREATING_A_NEW_CLAN));
			return;
		}
		if(clanName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAMES_LENGTH_IS_INCORRECT));
			return;
		}
		if(!StringUtil.isMatchingRegexp(clanName, Config.CLAN_NAME_TEMPLATE))
		{
			// clan name is not matching template
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT));
			return;
		}

		L2Clan clan = ClanTable.getInstance().createClan(player, clanName);
		if(clan == null)
		{
			// clan name is already taken
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT));
			return;
		}

		// should be update packet only
		player.sendPacket(new PledgeShowInfoUpdate(clan));
		player.sendPacket(new PledgeShowMemberListAll(clan, player));
		player.updatePledgeClass();
		player.broadcastUserInfo(true);
		player.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_BEEN_CREATED));
	}

	public void setLeader(L2Player player, String name)
	{
		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}

		L2Clan clan = player.getClan();

		if(clan == null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN));
			return;
		}

		L2ClanMember member = clan.getClanMember(name);
		if(member == null)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.S1IsNotMemberOfTheClan", player).addString(name));
			showChatWindow(player, "data/html/villagemaster/clan-20.htm");
			return;
		}

		if(Config.MAINTENANCE_DAY > 0)
		{
			if(member.getPledgeType() != 0)
			{
				showChatWindow(player, "data/html/villagemaster/clan-22.htm");
				return;
			}
			MaintenanceManager.getInstance().addTask("ChangeClanLeaderTask", clan.getClanId() + ":" + member.getObjectId());
		}
		else
		{
			clan.setLeader(member);
			player.setClanLeader(false);

			L2ItemInstance item = player.getInventory().getItemByItemId(6841);
			if(item != null && item.isEquipped())
				player.getInventory().unEquipItemAndSendChanges(item);

			if(member.getPlayer() != null)
				member.getPlayer().setClanLeader(true);

			clan.updateClanInDB();
		}

		player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.ClanLeaderWillBeChangedFromS1ToS2", player).addString(player.getName()).addString(name));
	}

	public void createSubPledge(L2Player player, String clanName, int pledgeType, int minClanLvl, String leaderName)
	{
		int subLeaderId = 0;
		L2ClanMember subLeader = null;

		L2Clan clan = player.getClan();

		if(clan == null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN);
			player.sendPacket(sm);
			return;
		}

		if(!player.isClanLeader())
		{
			SystemMessage sm = new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_CREATE_A_CLAN);
			player.sendPacket(sm);
			return;
		}

		if(!StringUtil.isMatchingRegexp(clanName, Config.CLAN_NAME_TEMPLATE))
		{
			SystemMessage sm = new SystemMessage(SystemMessage.CLAN_NAME_IS_INCORRECT);
			player.sendPacket(sm);
			return;
		}

		SubPledge[] subPledge = clan.getAllSubPledges();
		for(SubPledge element : subPledge)
			if(element.getName().equals(clanName))
			{
				SystemMessage sm = new SystemMessage(SystemMessage.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
				player.sendPacket(sm);
				return;
			}

		if(ClanTable.getInstance().getClanByName(clanName) != null)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.ANOTHER_MILITARY_UNIT_IS_ALREADY_USING_THAT_NAME_PLEASE_ENTER_A_DIFFERENT_NAME);
			player.sendPacket(sm);
			return;
		}

		if(clan.getLevel() < minClanLvl)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.THE_CONDITIONS_NECESSARY_TO_CREATE_A_MILITARY_UNIT_HAVE_NOT_BEEN_MET);
			player.sendPacket(sm);
			return;
		}

		if(pledgeType != L2Clan.SUBUNIT_ACADEMY)
		{
			subLeader = clan.getClanMember(leaderName);
			if(subLeader == null || subLeader.getPledgeType() != L2Clan.SUBUNIT_NONE)
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.PlayerCantBeAssignedAsSubUnitLeader", player));
				return;
			}
			else if(subLeader.isClanLeader())
			{
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.YouCantBeASubUnitLeader", player));
				return;
			}
			else
				subLeaderId = subLeader.getObjectId();
		}

		pledgeType = clan.createSubPledge(player, pledgeType, subLeaderId, clanName);
		if(pledgeType == L2Clan.SUBUNIT_NONE)
			return;

		clan.broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(clan.getSubPledge(pledgeType)));

		SystemMessage sm;
		if(pledgeType == L2Clan.SUBUNIT_ACADEMY)
		{
			sm = new SystemMessage(SystemMessage.CONGRATULATIONS_THE_S1S_CLAN_ACADEMY_HAS_BEEN_CREATED);
			sm.addString(clan.getName());
		}
		else if(pledgeType >= L2Clan.SUBUNIT_KNIGHT1)
		{
			sm = new SystemMessage(SystemMessage.THE_KNIGHTS_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(clan.getName());
		}
		else if(pledgeType >= L2Clan.SUBUNIT_ROYAL1)
		{
			sm = new SystemMessage(SystemMessage.THE_ROYAL_GUARD_OF_S1_HAVE_BEEN_CREATED);
			sm.addString(clan.getName());
		}
		else
			sm = new SystemMessage(SystemMessage.CLAN_HAS_BEEN_CREATED);

		player.sendPacket(sm);

		if(subLeader != null)
		{
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(subLeader));
			if(subLeader.isOnline())
			{
				subLeader.getPlayer().updatePledgeClass();
				subLeader.getPlayer().broadcastUserInfo(true);
			}
		}
	}

	public void assignSubPledgeLeader(L2Player player, String clanName, String leaderName)
	{
		L2Clan clan = player.getClan();

		if(clan == null)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.ClanDoesntExist", player));
			return;
		}

		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}

		SubPledge[] subPledge = clan.getAllSubPledges();
		int match = -1;
		for(int i = 0; i < subPledge.length; i++)
			if(subPledge[i].getName().equals(clanName))
			{
				match = i;
				break;
			}
		if(match < 0)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.SubUnitNotFound", player));
			return;
		}

		L2ClanMember subLeader = clan.getClanMember(leaderName);
		if(subLeader == null || subLeader.getPledgeType() != L2Clan.SUBUNIT_NONE)
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.PlayerCantBeAssignedAsSubUnitLeader", player));
			return;
		}

		if(subLeader.isClanLeader())
		{
			player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.YouCantBeASubUnitLeader", player));
			return;
		}

		subPledge[match].setLeaderId(subLeader.getObjectId());
		clan.broadcastToOnlineMembers(new PledgeReceiveSubPledgeCreated(subPledge[match]));

		clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(subLeader));
		if(subLeader.isOnline())
		{
			subLeader.getPlayer().updatePledgeClass();
			subLeader.getPlayer().broadcastUserInfo();
		}

		player.sendMessage(new CustomMessage("ru.l2gw.gameserver.model.instances.L2VillageMasterInstance.NewSubUnitLeaderHasBeenAssigned", player));
	}

	private void dissolveClan(L2Player player)
	{
		if(player == null || player.getClanId() <= 0)
			return;
		L2Clan clan = player.getClan();

		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}
		if(clan.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DISPERSE_THE_CLANS_IN_YOUR_ALLIANCE));
			return;
		}
		if(clan.isAtWar() > 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_DISSOLVE_A_CLAN_WHILE_ENGAGED_IN_A_WAR));
			return;
		}
		if(clan.getHasUnit(1) || clan.getHasUnit(2) || clan.getHasUnit(3))
		{
			player.sendPacket(new SystemMessage(SystemMessage.UNABLE_TO_DISPERSE_YOUR_CLAN_OWNS_ONE_OR_MORE_CASTLES_OR_HIDEOUTS));
			return;
		}
		if(SiegeDatabase.checkIsRegistered(player.getClanId(), null))
		{
			player.sendPacket(new SystemMessage(SystemMessage.UNABLE_TO_DISPERSE_YOUR_CLAN_HAS_REQUESTED_TO_PARTICIPATE_IN_A_CASTLE_SIEGE));
			return;
		}

		ClanTable.getInstance().dissolveClan(player);
	}

	public void levelUpClan(L2Player player)
	{
		if(player.getClanId() <= 0)
			return;
		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}

		boolean increaseClanLevel = false;
		L2Clan clan = player.getClan();
		switch(clan.getLevel())
		{
			case 0:
				// Upgrade to 1
				if(player.getSp() >= 20000 && player.getAdena() >= 650000)
				{
					player.setSp(player.getSp() - 20000);
					player.reduceAdena("ClanLevelUp", 650000, this, true);
					increaseClanLevel = true;
				}
				break;
			case 1:
				// Upgrade to 2
				if(player.getSp() >= 100000 && player.getAdena() >= 2500000)
				{
					player.setSp(player.getSp() - 100000);
					player.reduceAdena("ClanLevelUp", 2500000, this, true);
					increaseClanLevel = true;
				}
				break;
			case 2:
				// Upgrade to 3
				// itemid 1419 == Blood Mark
				if(player.getSp() >= 350000 && player.getInventory().getItemByItemId(1419) != null)
				{

					player.setSp(player.getSp() - 350000);
					player.destroyItemByItemId("ClanLevelUp", 1419, 1, this, true);
					increaseClanLevel = true;
				}
				break;
			case 3:
				// Upgrade to 4
				// itemid 3874 == Alliance Manifesto
				if(player.getSp() >= 1000000 && player.getInventory().getItemByItemId(3874) != null)
				{
					player.setSp(player.getSp() - 1000000);
					player.destroyItemByItemId("ClanLevelUp", 3874, 1, this, true);
					increaseClanLevel = true;
				}
				break;
			case 4:
				// Upgrade to 5
				// itemid 3870 == Seal of Aspiration
				if(player.getSp() >= 2500000 && player.getInventory().getItemByItemId(3870) != null)
				{
					player.setSp(player.getSp() - 2500000);
					player.destroyItemByItemId("ClanLevelUp", 3870, 1, this, true);
					increaseClanLevel = true;
				}
				break;
			case 5:
				// Upgrade to 6
				if(clan.getReputationScore() >= 5000 && clan.getMembersCount() >= Config.MemberForLevel6)
				{
					clan.incReputation(-5000, false, "LvlUpClan");
					increaseClanLevel = true;
				}
				break;
			case 6:
				// Upgrade to 7
				if(clan.getReputationScore() >= 10000 && clan.getMembersCount() >= Config.MemberForLevel7)
				{
					clan.incReputation(-10000, false, "LvlUpClan");
					increaseClanLevel = true;
				}
				break;
			case 7:
				// Upgrade to 8
				if(clan.getReputationScore() >= 20000 && clan.getMembersCount() >= Config.MemberForLevel8)
				{
					clan.incReputation(-20000, false, "LvlUpClan");
					increaseClanLevel = true;
				}
				break;
			case 8:
				// Upgrade to 9
				// itemId 9910 == Blood Oath
				if(clan.getReputationScore() >= 40000 && clan.getMembersCount() >= Config.MemberForLevel9)
				{
					L2ItemInstance item = player.getInventory().getItemByItemId(9910);
					if(item != null && item.getCount() >= 150)
					{
						clan.incReputation(-40000, false, "LvlUpClan");
						player.destroyItemByItemId("ClanLevelUp", 9910, 150, this, true);
						increaseClanLevel = true;
					}
				}
				break;
			case 9:
				// Upgrade to 10
				// itemId 9911 == Blood Alliance
				if(clan.getReputationScore() >= 40000 && clan.getMembersCount() >= Config.MemberForLevel10)
				{
					L2ItemInstance item = player.getInventory().getItemByItemId(9911);
					if(item != null && item.getCount() >= 5)
					{
						clan.incReputation(-40000, false, "LvlUpClan");
						player.destroyItemByItemId("ClanLevelUp", 9911, 5, this, true);
						increaseClanLevel = true;
					}
				}
				break;
			case 10:
				// Upgrade to 11
				if(clan.getReputationScore() >= 75000 && clan.getMembersCount() >= Config.MemberForLevel11 && clan.getHasCastle() > 0 && clan.getLeader().getVarB("territory_lord_" + (80 + clan.getHasCastle())))
				{
					clan.incReputation(-75000, false, "LvlUpClan");
					increaseClanLevel = true;
				}
				break;
		}

		if(increaseClanLevel)
		{
			player.sendChanges();

			clan.setLevel((byte) (clan.getLevel() + 1));
			clan.updateClanInDB();
			doCast(SkillTable.getInstance().getInfo(5103, 1), player, null, true);

			if(Config.PREMIUM_ENABLED && Config.PREMIUM_MIN_CLAN_LEVEL >= clan.getLevel())
				player.startPremiumTask(0);

			if(clan.getLevel() > 3)
				SiegeManager.addSiegeSkills(player);

			if(clan.getLevel() == 5)
				player.sendPacket(new SystemMessage(SystemMessage.NOW_THAT_YOUR_CLAN_LEVEL_IS_ABOVE_LEVEL_5_IT_CAN_ACCUMULATE_CLAN_REPUTATION_POINTS));

			// notify all the members about it
			final SystemMessage sm = new SystemMessage(SystemMessage.CLANS_SKILL_LEVEL_HAS_INCREASED);
			final PledgeShowInfoUpdate pu = new PledgeShowInfoUpdate(clan);
			for(L2Player member : clan.getOnlineMembers(""))
				if(member.isOnline())
				{
					member.updatePledgeClass();
					member.sendPacket(sm);
					member.sendPacket(pu);
					member.getPlayer().broadcastUserInfo(true);
				}
		}
		else
			player.sendPacket(new SystemMessage(SystemMessage.CLAN_HAS_FAILED_TO_INCREASE_SKILL_LEVEL));
	}

	public void createAlly(L2Player player, String allyName)
	{
		// D5 You may not ally with clan you are battle with.
		// D6 Only the clan leader may apply for withdraw from alliance.
		// DD No response. Invitation to join an
		// D7 Alliance leaders cannot withdraw.
		// D9 Different Alliance
		// EB alliance information
		// Ec alliance name $s1
		// ee alliance leader: $s2 of $s1
		// ef affilated clans: total $s1 clan(s)
		// f6 you have already joined an alliance
		// f9 you cannot new alliance 10 days
		// fd cannot accept. clan ally is register as enemy during siege battle.
		// fe you have invited someone to your alliance.
		// 100 do you wish to withdraw from the alliance
		// 102 enter the name of the clan you wish to expel.
		// 202 do you realy wish to dissolve the alliance
		// 502 you have accepted alliance
		// 602 you have failed to invite a clan into the alliance
		// 702 you have withdraw

		if(Config.DEBUG)
			_log.debug(player.getObjectId() + "(" + player.getName() + ") requested ally creation from " + getObjectId() + "(" + getName() + ")");

		if(!player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES));
			return;
		}
		if(player.getAllyId() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_ALREADY_BELONG_TO_ANOTHER_ALLIANCE));
			return;
		}
		if(allyName.length() > 16)
		{
			player.sendPacket(new SystemMessage(SystemMessage.INCORRECT_LENGTH_FOR_AN_ALLIANCE_NAME));
			return;
		}
		if(!StringUtil.isMatchingRegexp(allyName, Config.ALLY_NAME_TEMPLATE))
		{
			player.sendPacket(new SystemMessage(SystemMessage.INCORRECT_ALLIANCE_NAME));
			return;
		}
		L2Clan clan = player.getClan();
		if(clan.getLevel() < 5)
		{
			player.sendPacket(new SystemMessage(SystemMessage.TO_CREATE_AN_ALLIANCE_YOUR_CLAN_MUST_BE_LEVEL_5_OR_HIGHER));
			return;
		}
		if(ClanTable.getInstance().getAllyByName(allyName) != null)
		{
			player.sendPacket(new SystemMessage(SystemMessage.THIS_ALLIANCE_NAME_ALREADY_EXISTS));
			return;
		}
		if(!clan.canCreateAlly())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_CREATE_A_NEW_ALLIANCE_WITHIN_10_DAYS_AFTER_DISSOLUTION));
			return;
		}

		L2Alliance alliance = ClanTable.getInstance().createAlliance(player, allyName);
		if(alliance == null)
			return;

		player.broadcastUserInfo(true);
		player.sendMessage("Alliance " + allyName + " has been created.");
	}

	private void dissolveAlly(L2Player player)
	{
		if(player == null || player.getAllyId() <= 0)
			return;

		if(!player.isAllyLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.FEATURE_AVAILABLE_TO_ALLIANCE_LEADERS_ONLY));
			return;
		}

		if(player.getAlliance().getMembersCount() > 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_FAILED_TO_DISSOLVE_THE_ALLIANCE));
			return;
		}

		ClanTable.getInstance().dissolveAlly(player);
	}

	private Race getVillageMasterRace()
	{
		switch(getTemplate().getRace())
		{
			case 14:
				return Race.human;
			case 15:
				return Race.elf;
			case 16:
				return Race.darkelf;
			case 17:
				return Race.orc;
			case 18:
				return Race.dwarf;
			case 25:
				return Race.kamael;
		}
		return null;
	}

	public boolean checkSubChangeCond(L2Player player)
	{
		if(player.isPetSummoned())
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_SUB_CLASS_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SERVITOR_OR_PET_IS_SUMMONED));
			return false;
		}

		// Саб класс нельзя получить или поменять, пока используется скилл или персонаж находится в режиме трансформации
		if(player.isActionsDisabled() || player.getTransformation() != 0)
		{
			player.sendPacket(new SystemMessage(SystemMessage.SUB_CLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE));
			return false;
		}

		if(Olympiad.isRegisteredInComp(player) || player.getOlympiadGameId() > -1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.C1_IS_ALREADY_REGISTERED_ON_THE_MATCH_WAITING_LIST).addCharName(player));
			return false;
		}

		if(player.getWeightPenalty() >= 3)
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_WHILE_YOU_ARE_OVER_YOUR_WEIGHT_LIMIT));
			return false;
		}

		if(player.getInventoryLimit() * 0.8 < player.getInventoryItemsCount())
		{
			player.sendPacket(new SystemMessage(SystemMessage.A_SUB_CLASS_CANNOT_BE_CREATED_OR_CHANGED_BECAUSE_YOU_HAVE_EXCEEDED_YOUR_INVENTORY_LIMIT));
			return false;
		}

		return true;
	}
}
