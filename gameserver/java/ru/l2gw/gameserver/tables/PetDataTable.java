package ru.l2gw.gameserver.tables;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.gameserver.templates.L2PetTemplate;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Fully Rewrite by: 
 * @author FlareDrakon
 * for kill hard code
 * 22.03.2009
 * replaced data is xml
 */
public class PetDataTable
{
	private static final org.apache.commons.logging.Log _log = LogFactory.getLog(PetDataTable.class.getName());

	private static PetDataTable _instance = new PetDataTable();
	private static TIntObjectHashMap<L2PetTemplate> _pets;

	public final static int PET_WOLF_ID = 12077;

	public final static int HATCHLING_WIND_ID = 12311;
	public final static int HATCHLING_STAR_ID = 12312;
	public final static int HATCHLING_TWILIGHT_ID = 12313;

	public final static int STRIDER_WIND_ID = 12526;
	public final static int STRIDER_STAR_ID = 12527;
	public final static int STRIDER_TWILIGHT_ID = 12528;
	public final static int GUARDIANS_STRIDER_ID = 16068;


	public final static int RED_STRIDER_WIND_ID = 16038;
	public final static int RED_STRIDER_STAR_ID = 16039;
	public final static int RED_STRIDER_TWILIGHT_ID = 16040;

	public final static int WYVERN_ID = 12621;

	public final static int BABY_BUFFALO_ID = 12780;
	public final static int BABY_KOOKABURRA_ID = 12781;
	public final static int BABY_COUGAR_ID = 12782;

	public final static int IMPROVED_BABY_BUFFALO_ID = 16034;
	public final static int IMPROVED_BABY_KOOKABURRA_ID = 16035;
	public final static int IMPROVED_BABY_COUGAR_ID = 16036;

	public final static int SIN_EATER_ID = 12564;

	public final static int AGATION_WOLF_ID = 16030;
	public final static int BLACK_WOLF_ID = 16025;
	public final static int WGREAT_WOLF_ID = 16037;
	public final static int FENRIR_WOLF_ID = 16041;
	public final static int WFENRIR_WOLF_ID = 16042;
	public final static int GRAY_HORSE_ID = 13130;

	public static PetDataTable getInstance()
	{
		return _instance;
	}

	public PetDataTable()
	{
		if(_pets == null)
			_pets = new TIntObjectHashMap<>();

		FillPetDataTable();
	}

	public L2PetTemplate getInfo(final int petNpcId, int level)
	{
		if(_pets == null)
		{
			PetDataTable.getInstance();
		}

		L2PetTemplate result = null;
		while(result == null && level < 100)
		{
			result = _pets.get(petNpcId * 100 + level);
			level++;
		}

		return result;
	}

	public void FillPetDataTable()
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		try
		{
			final File file = new File(Config.DATAPACK_ROOT + "/data/pet_data.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("Pet Data : NO FILE");
				return;
			}

			final Document doc = factory.newDocumentBuilder().parse(file);
			for(Node l = doc.getFirstChild(); l != null; l = l.getNextSibling())
				if("list".equalsIgnoreCase(l.getNodeName()))
					for(Node pet = l.getFirstChild(); pet != null; pet = pet.getNextSibling())
						if("pet".equalsIgnoreCase(pet.getNodeName()))
						{
							NamedNodeMap attr = pet.getAttributes();
							String type = attr.getNamedItem("type").getNodeValue();
							int id = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
							int ride_state = attr.getNamedItem("ride_state") != null ? L2Skill.RideState.valueOf(attr.getNamedItem("ride_state").getNodeValue()).mask : 0;
							L2NpcTemplate npcTemplate = NpcTable.getTemplate(id);

							if(npcTemplate == null)
							{
								_log.warn("PetDataTable: no npc template for npc id: " + id);
								continue;
							}

							int index = Integer.parseInt(attr.getNamedItem("index").getNodeValue());
							int controlItemId = Integer.parseInt(attr.getNamedItem("controlItemId").getNodeValue());
							int syncLevel = Integer.parseInt(attr.getNamedItem("sync_level").getNodeValue());

							for(Node petlvl = pet.getFirstChild(); petlvl != null; petlvl = petlvl.getNextSibling())
								if("level".equalsIgnoreCase(petlvl.getNodeName()))
								{
									NamedNodeMap a = petlvl.getAttributes();
									L2PetTemplate petTemplate = new L2PetTemplate(npcTemplate.getSet(), npcTemplate.getAIParams());
									petTemplate.setSkills(npcTemplate.getSkills());
									petTemplate.index = index;
									petTemplate.controlItemId = controlItemId;
									petTemplate.sync_level = syncLevel > 0;
									petTemplate.level = Integer.parseInt(a.getNamedItem("petlevel").getNodeValue());
									petTemplate.meal_in_battle = Integer.parseInt(a.getNamedItem("consume_meal_in_battle").getNodeValue());
									petTemplate.meal_in_normal = Integer.parseInt(a.getNamedItem("consume_meal_in_normal").getNodeValue());
									petTemplate.exp = Long.parseLong(a.getNamedItem("exp").getNodeValue());
									petTemplate.food = new GArray<Integer>(1);
									String[] food = a.getNamedItem("food").getNodeValue().split(";");
									if(food.length > 0)
										for(String foodId : food)
											if(foodId != null && !foodId.isEmpty())
												petTemplate.food.add(Integer.parseInt(foodId));
									petTemplate.exp_type = (100 - Integer.parseInt(a.getNamedItem("get_exp_type").getNodeValue())) / 100f;
									petTemplate.hungry_limit = Integer.parseInt(a.getNamedItem("hungry_limit").getNodeValue()) / 100f;
									petTemplate.max_meal = Integer.parseInt(a.getNamedItem("max_meal").getNodeValue()); 
									petTemplate.org_hp = Formulas.getMaxHpFromBase(Float.parseFloat(a.getNamedItem("org_hp").getNodeValue()), npcTemplate.baseCON);
									petTemplate.org_hp_regen = Float.parseFloat(a.getNamedItem("org_hp_regen").getNodeValue());
									petTemplate.org_mattack = (int) Float.parseFloat(a.getNamedItem("org_mattack").getNodeValue());
									petTemplate.org_mdefend = (int) Float.parseFloat(a.getNamedItem("org_mdefend").getNodeValue());
									petTemplate.org_mp = Formulas.getMaxMpFromBase(Float.parseFloat(a.getNamedItem("org_mp").getNodeValue()), npcTemplate.baseMEN);
									petTemplate.org_mp_regen = Float.parseFloat(a.getNamedItem("org_mp_regen").getNodeValue());
									petTemplate.org_pattack = (int) Float.parseFloat(a.getNamedItem("org_pattack").getNodeValue());
									petTemplate.org_pdefend = (int) Float.parseFloat(a.getNamedItem("org_pdefend").getNodeValue());
									petTemplate.soulshot_count = Integer.parseInt(a.getNamedItem("soulshot_count").getNodeValue());
									petTemplate.spiritshot_count = Integer.parseInt(a.getNamedItem("spiritshot_count").getNodeValue());

									petTemplate.attack_speed_on_ride = a.getNamedItem("attack_speed_on_ride") != null ? Integer.parseInt(a.getNamedItem("attack_speed_on_ride").getNodeValue()) : 0; 
									petTemplate.meal_in_battle_on_ride = a.getNamedItem("consume_meal_in_battle_on_ride") != null ? Integer.parseInt(a.getNamedItem("consume_meal_in_battle_on_ride").getNodeValue()) : 0;
									petTemplate.meal_in_normal_on_ride = a.getNamedItem("consume_meal_in_normal_on_ride") != null ? Integer.parseInt(a.getNamedItem("consume_meal_in_normal_on_ride").getNodeValue()) : 0;
									petTemplate.mattack_on_ride = a.getNamedItem("mattack_on_ride") != null ? (int)(Float.parseFloat(a.getNamedItem("mattack_on_ride").getNodeValue())) : 0;
									petTemplate.pattack_on_ride = a.getNamedItem("pattack_on_ride") != null ? (int)(Float.parseFloat(a.getNamedItem("pattack_on_ride").getNodeValue())) : 0; 
									petTemplate.ride_state = ride_state;

									if(a.getNamedItem("speed_on_ride") != null)
									{
										String[] speed = a.getNamedItem("speed_on_ride").getNodeValue().split(";");
										petTemplate.speed_on_ride_g = Integer.parseInt(speed[0]);
										petTemplate.speed_on_ride_s = Integer.parseInt(speed[3]);
										petTemplate.speed_on_ride_f = Integer.parseInt(speed[4]);
									}

									/*
									if(type != null)//TODO: тож в xml снести
									{
										if(type.equals("wolf"))
										{
											petData.setItemType(6);
											petData.setType(L2PetData.PetType.WOLF);
										}
										else if(type.startsWith("hatchling"))
										{
											petData.setItemType(7);
											petData.setType(L2PetData.PetType.HETCHING);
										}
										else if(type.contains("strider"))
										{
											petData.setItemType(8);
											petData.setType(L2PetData.PetType.STRIDER);
										}
										else if(type.contains("Great") || type.contains("Fenrir"))
										{
											petData.setItemType(10);
											petData.setType(L2PetData.PetType.GWOLF);
										}
										else if(isBaby || isImproved)
										{
											petData.setItemType(12);
											petData.setType(L2PetData.PetType.BABY);
										}
										else
										{
											petData.setItemType(0);
											petData.setType(L2PetData.PetType.NONE);
										}
									}
									*/
									_pets.put(petTemplate.npcId * 100 + petTemplate.level, petTemplate);
								}
						}
		}
		catch(final Exception e)
		{
			_log.warn("Cannot fill up PetDataTable: " + e);
		}

		_log.info("PetDataTable: Loaded " + _pets.size() + " pets.");
	}

	public static void deletePet(final L2ItemInstance item, final L2Character owner)
	{
		int petObjectId = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId FROM pets WHERE item_obj_id=?");
			statement.setInt(1, item.getObjectId());
			rset = statement.executeQuery();
			while(rset.next())
				petObjectId = rset.getInt("objId");
			DbUtils.closeQuietly(statement, rset);

			final L2Summon summon = owner.getPet();
			if(summon != null && summon.getObjectId() == petObjectId)
				summon.unSummon();

			final L2Player player = owner.getPlayer();
			if(player != null && player.getMountEngine().isMounted() && player.getMountEngine().getMountObjId() == petObjectId)
				player.getMountEngine().dismount();

			// if it's a pet control item, delete the pet
			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
			statement.setInt(1, item.getObjectId());
			statement.execute();
		}
		catch(final Exception e)
		{
			_log.warn("could not restore pet objectid:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static void unSummonPet(final L2ItemInstance oldItem, final L2Character owner)
	{
		int petObjectId = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT objId FROM pets WHERE item_obj_id=?");
			statement.setInt(1, oldItem.getObjectId());
			rset = statement.executeQuery();

			while(rset.next())
				petObjectId = rset.getInt("objId");

			if(owner == null)
				return;

			final L2Summon summon = owner.getPet();
			if(summon != null && summon.getObjectId() == petObjectId)
				summon.unSummon();

			final L2Player player = owner.getPlayer();
			if(player != null && player.getMountEngine().isMounted() && player.getMountEngine().getMountObjId() == petObjectId)
				player.getMountEngine().dismount();
		}
		catch(final Exception e)
		{
			_log.warn("could not restore pet objectid:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public static int getControlItemId(final int npcId)
	{
		if(_pets == null)
		{
			PetDataTable.getInstance();
		}

		for(final L2PetTemplate pet : _pets.valueCollection())
		{
			if(pet.npcId == npcId)
				return pet.controlItemId;
		}
		return 1;
	}

	public static int getSummonId(final L2ItemInstance item)
	{
		if(_pets == null)
		{
			PetDataTable.getInstance();
		}

		for(final L2PetTemplate pet : _pets.valueCollection())
			if(pet.controlItemId == item.getItemId())
				return pet.npcId;
		return 0;
	}

	public static boolean isPetControlItem(final L2ItemInstance item)
	{
		if(_pets == null)
		{
			PetDataTable.getInstance();
		}

		for(final L2PetTemplate pet : _pets.valueCollection())
			if(pet.controlItemId == item.getItemId())
				return true;
		return false;
	}
}