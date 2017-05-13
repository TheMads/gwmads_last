package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.extensions.scripts.Scripts;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.instancemanager.QuestManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ShortCut;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.quest.Quest;
import ru.l2gw.gameserver.model.quest.QuestState;
import ru.l2gw.gameserver.network.GameClient;
//import ru.l2gw.gameserver.serverpackets.CharacterCreateFail;
import ru.l2gw.gameserver.serverpackets.CharacterCreateSuccess;
import ru.l2gw.gameserver.serverpackets.CharacterSelectionInfo;
import ru.l2gw.gameserver.tables.CharNameTable;
import ru.l2gw.gameserver.tables.CharTemplateTable;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2PlayerTemplate;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.util.Location;

public class CharacterCreate extends L2GameClientPacket
{
	// cSdddddddddddd
	private String _name;
	private int _sex;
	private int _race;
	private int _classId;
	private int _hairStyle;
	private int _hairColor;
	private int _face;
	private String Start;

	@Override
	public void readImpl()
	{
		_name = readS();
		readD(); // race
		_sex = readD();
		_classId = readD();
		readD(); // int
		readD(); // str
		readD(); // con
		readD(); // men
		readD(); // dex
		readD(); // wit
		_hairStyle = readD();
		_hairColor = readD();
		_face = readD();
	}

	@Override
	public void runImpl()
	{
		for(ClassId cid : ClassId.values())
			if(cid.getId() == _classId && cid.getLevel() != 1)
				return;
		if(CharNameTable.getInstance().accountCharNumber(getClient().getLoginName()) >= 8)
		{
			//sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_TOO_MANY_CHARACTERS));
			return;
		}
		if(!StringUtil.isMatchingRegexp(_name, Config.EnTemplate))
			{
				//sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_16_ENG_CHARS));
				return;
			}
		if(CharNameTable.getInstance().doesCharNameExist(_name))
		{
			//sendPacket(new CharacterCreateFail(CharacterCreateFail.REASON_NAME_ALREADY_EXISTS));
			return;
		}

		L2Player newChar = L2Player.create((short) _classId, (byte) _sex, getClient().getLoginName(), _name, (byte) _hairStyle, (byte) _hairColor, (byte) _face);
		if(newChar == null)
			return;

		sendPacket(CharacterCreateSuccess.STATIC);

		initNewChar(getClient(), newChar);
	}

	private void initNewChar(GameClient client, L2Player newChar)
	{
		L2PlayerTemplate template = newChar.getTemplate();

		L2Player.restoreCharSubClasses(newChar);
		Location startPoint = CharTemplateTable.getInitialStartPoint(template.classId);

		if(Config.STARTING_ADENA > 0)
			newChar.addAdena("NewChar", Config.STARTING_ADENA, null, false);

		newChar.setXYZInvisible(startPoint.getX(), startPoint.getY(), startPoint.getZ());
		newChar.setTitle("");

		GArray<StatsSet> items = Config.CUSTOM_INITIAL_EQUIPMENT ? CharTemplateTable.getInitialCustomEquipment(template.classId) : CharTemplateTable.getInitialEquipment(template.classId);
		if(items != null)
			for(StatsSet itemInfo : items)
			{
				try
				{
					L2ItemInstance item = ItemTable.getInstance().createItem("CharacterCreate", itemInfo.getInteger("itemId"), itemInfo.getLong("count"), newChar, null);
					newChar.getInventory().addItem("NewChar", item, newChar, null);

					if(item.getItemId() == 5588) // tutorial book
						newChar.registerShortCut(new L2ShortCut(11, 0, 1, item.getObjectId(), -1));

					if(item.isEquipable())
						if(newChar.getInventory().getItemInBodySlot(item.getBodyPart()) == null)
							newChar.getInventory().equipItem(item);
				}
				catch(Exception e)
				{
					// quite
				}
			}

		if(Config.START_ITEMS.length > 1)
			try
			{
				for(int i = 0; i < Config.START_ITEMS.length; i += 2)
					newChar.getInventory().addItem("StartItems", Config.START_ITEMS[i], Config.START_ITEMS[i + 1], newChar, null);
			}
			catch (Exception e)
			{
				_log.warn("CharacterCreate: error adding start items: " + e, e);
			}

		// add attack, take, sit shortcut
		newChar.registerShortCut(new L2ShortCut(0, 0, 3, 2, -1));
		newChar.registerShortCut(new L2ShortCut(3, 0, 3, 5, -1));
		newChar.registerShortCut(new L2ShortCut(10, 0, 3, 0, -1));
		
		for(L2Skill skill : newChar.getAllSkills())
		{
			int skillId = skill.getId();
			if(skillId == 1001 || skillId == 1177)
				newChar.registerShortCut(new L2ShortCut(1, 0, 2, skillId, 1));

			if(skillId == 1216)
				newChar.registerShortCut(new L2ShortCut(9, 0, 2, skillId, 1));
		}

		if(Config.CHARACTER_CREATE_LEVEL > 1)
		{
			newChar.setExp(Experience.LEVEL[Config.CHARACTER_CREATE_LEVEL]);
			newChar.setLevel(Config.CHARACTER_CREATE_LEVEL);
		}
		else if(Config.FIRST_CHARACTER_LEVEL > 0 && CharNameTable.getInstance().accountCharNumber(getClient().getLoginName()) == 1)
		{
			newChar.setExp(Experience.LEVEL[Config.FIRST_CHARACTER_LEVEL]);
			newChar.setLevel(Config.FIRST_CHARACTER_LEVEL);
			if(Config.FIRST_CHARACTER_WH_ADENA > 0)
			{
				L2ItemInstance adena = ItemTable.getInstance().createItem("FirstChar", 57, Config.FIRST_CHARACTER_WH_ADENA, newChar, null);
				adena.setOwnerId(newChar.getObjectId());
				adena.setLocation(L2ItemInstance.ItemLocation.WAREHOUSE);
				adena.updateDatabase(true);
			}
		}

		startTutorialQuest(newChar);

		newChar.saveCharToDisk();

		Object[] script_args = new Object[] { newChar };
		for(Scripts.ScriptClassAndMethod handler : Scripts.onLevelUp)
			newChar.callScripts(handler.scriptClass, handler.method, script_args);

		newChar.deleteMe(); // release the world of this character and it's inventory

		CharacterSelectionInfo cl = new CharacterSelectionInfo(client.getLoginName(), client.getSessionId().playOkID1);
		client.getConnection().sendPacket(cl);
		client.setCharSelection(cl.getCharInfo());
	}

	public static void startTutorialQuest(L2Player player)
	{
		Quest q = QuestManager.getQuest(255);
		if(q != null)
		{
			QuestState st = q.newQuestState(player);
			st.set("t1", -1);
			st.setCond(1);
			st.setState(Quest.STARTED);
		}
	}
}
