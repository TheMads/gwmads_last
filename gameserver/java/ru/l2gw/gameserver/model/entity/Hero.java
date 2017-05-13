package ru.l2gw.gameserver.model.entity;

import javolution.text.TextBuilder;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadTeam;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadUserInfo;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.PledgeShowInfoUpdate;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.ClanTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.StatsSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;


public class Hero
{
	private static Log _log = LogFactory.getLog(Hero.class.getName());

	private static final int[] _heroItems = {6842, 6611, 6612, 6613, 6614, 6615, 6616,
			6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390};
	private static Map<Integer, StatsSet> _heroes;
	private static Map<Integer, List<StatsSet>> _heroHistory;
	private static Map<Integer, List<StatsSet>> _heroMatchHistory;

	protected static final Log _olyLog = LogFactory.getLog("olymp");

	public static Map<Integer, StatsSet> getHeroes()
	{
		return _heroes;
	}

	public static Map<Integer, StatsSet> getActiveHeroes()
	{
		Map<Integer, StatsSet> activeHero = new FastMap<Integer, StatsSet>();
		if(_heroes == null)
			return activeHero;

		for(int heroId : _heroes.keySet())
			if(_heroes.get(heroId).getInteger("active") == 1)
				activeHero.put(heroId, _heroes.get(heroId));

		return activeHero;
	}

	public static void computeNewHeroes(int currCycle)
	{
		_olyLog.warn("Compute new heroes started for cycle " + (currCycle + 1));
		removeHeroes();
		Map<Integer, StatsSet> newHeroes = new FastMap<Integer, StatsSet>();
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement stmt;
			ResultSet rset;

			_olyLog.info("Clean previos month nobles statistic.");
			stmt = con.prepareStatement("DELETE FROM olymp_nobles_prev");
			stmt.execute();
			stmt.close();

			_olyLog.info("Add current month nobles statistic to previus.");
			stmt = con.prepareStatement("INSERT INTO olymp_nobles_prev SELECT * FROM olymp_nobles WHERE wins + loos >= " + Config.ALT_OLY_MIN_MATCHES);
			stmt.execute();
			stmt.close();

			final String sql = "SELECT * FROM olymp_nobles WHERE class_id=? and wins > 0 and wins + loos >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY points DESC, wins DESC, loos DESC";
			final String sql2 = "SELECT * FROM olymp_nobles WHERE (class_id=132 or class_id=133) and wins > 0 and wins + loos >= " + Config.ALT_OLY_MIN_MATCHES + " ORDER BY points DESC, wins DESC, loos DESC";

			for(int classId = 88; classId <= 134; classId++)
			{
				if((classId > 118 && classId < 131) || classId == 133)
					continue;

				_olyLog.warn("Compute heroes for class " + classId + " started...");
				if(classId == 132)
					stmt = con.prepareStatement(sql2);
				else
				{
					stmt = con.prepareStatement(sql);
					stmt.setInt(1, classId);
				}
				rset = stmt.executeQuery();
				int lastPoints = -1;
				List<StatsSet> heroesInClass = new FastList<StatsSet>();

				while(rset.next())
				{
					StatsSet hero = new StatsSet();
					hero.set("class_id", rset.getInt("class_id"));
					hero.set("char_id", rset.getInt("char_id"));
					hero.set("char_name", rset.getString("char_name"));
					hero.set("wins", rset.getString("wins"));
					hero.set("loos", rset.getString("loos"));
					hero.set("points", rset.getInt("points"));
					if(lastPoints == -1)
						lastPoints = rset.getInt("points");
					else if(lastPoints != rset.getInt("points"))
						break;
					heroesInClass.add(hero);
				}
				rset.close();
				stmt.close();
				_olyLog.warn("Compute heroes in class " + (classId == 132 ? classId+"/133" : classId) + " " + heroesInClass.size());

				if(heroesInClass.size() > 0)
				{
					if(heroesInClass.size() == 1)
					{
						_olyLog.warn("Compute one hero in class " + (classId == 132 ? classId+"/133" : classId) + " " + heroesInClass.get(0).getString("char_name") + " " + heroesInClass.get(0).getInteger("points") +
								"/" + heroesInClass.get(0).getInteger("wins") + "/" + heroesInClass.get(0).getInteger("loos"));
						newHeroes.put(heroesInClass.get(0).getInteger("class_id"), heroesInClass.get(0));
					}
					else if(heroesInClass.size() > 1)
					{
						_olyLog.warn("Compute more then one hero in class " + (classId == 132 ? classId+"/133" : classId));
						if(heroesInClass.get(0).getInteger("wins") == heroesInClass.get(1).getInteger("wins"))
						{
							double pp1 = 100 / (heroesInClass.get(0).getInteger("wins") + heroesInClass.get(0).getInteger("loos")) * heroesInClass.get(0).getInteger("wins");
							double pp2 = 100 / (heroesInClass.get(1).getInteger("wins") + heroesInClass.get(1).getInteger("loos")) * heroesInClass.get(1).getInteger("wins");
							if(pp1 > pp2)
							{
								_olyLog.warn("Compute hero in class " + (classId == 132 ? classId+"/133" : classId) + " is " + heroesInClass.get(0).getString("char_name") + " " + heroesInClass.get(0).getInteger("points") +
										"/" + heroesInClass.get(0).getInteger("wins") + "/" + heroesInClass.get(0).getInteger("loos") + "/" + pp1 + "%");
								newHeroes.put(heroesInClass.get(0).getInteger("class_id"), heroesInClass.get(0));
							}
							else if(pp1 < pp2)
							{
								_olyLog.warn("Compute hero in class " + (classId == 132 ? classId+"/133" : classId) + " is " + heroesInClass.get(1).getString("char_name") + " " + heroesInClass.get(1).getInteger("points") +
										"/" + heroesInClass.get(1).getInteger("wins") + "/" + heroesInClass.get(1).getInteger("loos") + "/" + pp1 + "%");
								newHeroes.put(heroesInClass.get(1).getInteger("class_id"), heroesInClass.get(1));
							}
							else
								_olyLog.warn("Compute hero in class " + (classId == 132 ? classId+"/133" : classId) + " no hero in class");
						}
						else
						{
							_olyLog.warn("Compute hero in class " + (classId == 132 ? classId+"/133" : classId) + " is " + heroesInClass.get(0).getString("char_name") + " " + heroesInClass.get(0).getInteger("points") +
									"/" + heroesInClass.get(0).getInteger("wins") + "/" + heroesInClass.get(0).getInteger("loos") + " by wins count");
							newHeroes.put(heroesInClass.get(0).getInteger("class_id"), heroesInClass.get(0));
						}
					}
				}
			}

		}
		catch(SQLException e)
		{
			_log.warn("Olympiad System: Couldnt restore nobles data " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		if(newHeroes.size() > 0)
		{
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				PreparedStatement stmt;
				final String sql = "INSERT INTO hero_history VALUES(?,?,?,?,?,?,?)";
				int mons = Integer.parseInt(String.format("%04d%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)));

				stmt = con.prepareStatement("DELETE FROM hero_history WHERE mons=" + mons + "");
				stmt.execute();
				stmt.close();

				for(int classId : newHeroes.keySet())
				{
					StatsSet hero = newHeroes.get(classId);
					stmt = con.prepareStatement(sql);
					stmt.setInt(1, hero.getInteger("char_id"));
					stmt.setString(2, hero.getString("char_name"));
					stmt.setInt(3, hero.getInteger("class_id"));
					stmt.setInt(4, currCycle + 1);
					stmt.setInt(5, mons);
					stmt.setInt(6, 0);
					stmt.setString(7, "");
					stmt.execute();
					stmt.close();
				}
			}
			catch(SQLException e)
			{
				_log.warn("Couldnt store new heroes to db ");
				e.printStackTrace();
			}
			finally
			{
				try
				{
					con.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		loadHeroes();
	}

	public static void removeHeroes()
	{
		_olyLog.warn("Remove all heroes");

		if(_heroes != null && _heroes.size() != 0)
		{
			Connection con = null;
			try
			{
				con = DatabaseFactory.getInstance().getConnection();
				for(StatsSet hero : _heroes.values())
				{
					_olyLog.info("Remove hero from: " + hero.getString("char_name"));
					L2Player player = L2ObjectsStorage.getPlayer(hero.getString("char_name"));
					if(player != null)
					{
						try
						{
							synchronized(player)
							{
								player.setHero(false);
								for(int heroItemId : _heroItems)
								{
									L2ItemInstance heroItem;
									if((heroItem = player.getInventory().getItemByItemId(heroItemId)) != null)
									{
										if(heroItem.isEquipped())
											player.getInventory().unEquipItemAndSendChanges(heroItem);

										_olyLog.info("Remove hero item from online: " + player + " itemId: " + heroItemId);
										player.destroyItem("RemoveHero", heroItem.getObjectId(), heroItem.getCount(), null, true);
									}

									if((heroItem = player.getWarehouse().getItemByItemId(heroItemId)) != null)
									{
										_olyLog.info("Remove hero item from wh online: " + player + " itemId: " + heroItemId);
										player.getWarehouse().destroyItem("RemoveHero", heroItem, player, null);
									}
								}
								player.unEquipInappropriateItems();
							}

							if(!player.isSubClassActive())
							{
								_olyLog.info("Remove hero skills: " + player);
								player.removeSkillById(395);
								player.removeSkillById(396);
								player.removeSkillById(1374);
								player.removeSkillById(1375);
								player.removeSkillById(1376);
								player.sendPacket(new SkillList(player));
							}

							player.broadcastUserInfo(true);
						}
						catch(NullPointerException e)
						{
							_olyLog.warn("Error while deleting hero item for player " + e);
						}
					}
					else
					{
						_olyLog.info("Remove hero items from DB: player " + hero.getString("char_name"));
						PreparedStatement stmt;
						stmt = con.prepareStatement("DELETE FROM items WHERE owner_id = ? and item_id IN (6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390)");
						stmt.setInt(1, hero.getInteger("char_id"));
						stmt.execute();
						stmt.close();
					}
				}
			}
			catch(SQLException e)
			{
				_olyLog.warn("can't remove hero items from db " + e);
			}
			finally
			{
				try
				{
					con.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			_heroes = null;
		}
	}

	public static void loadHeroes()
	{
		_heroes = new FastMap<Integer, StatsSet>();

		PreparedStatement stmt;
		ResultSet rset;

		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("SELECT " +
					"  hh1.char_name,hh1.char_id,hh1.class_id," +
					"  c.clanid," +
					"  cd.ally_id," +
					"  hh1.active," +
					"  hh1.message," +
					"  count('hh2.*') as cnt " +
					"FROM " +
					"  hero_history hh1 " +
					"    left outer join hero_history hh2 on (hh1.char_id = hh2.char_id)" +
					"    inner join characters c on (c.obj_id = hh1.char_id)" +
					"    left outer join clan_data cd on (c.clanid = cd.clan_id) " +
					"WHERE " +
					"  hh1.mons=? " +
					"GROUP BY 1,2,3,4,5,6");

			int mons = Integer.parseInt(String.format("%04d%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)));

			stmt.setInt(1, mons);
			_olyLog.info("Loading heroes for cycle: " + mons);

			rset = stmt.executeQuery();

			while(rset.next())
			{
				StatsSet hero = new StatsSet();
				int charId = rset.getInt("char_id");
				hero.set("char_name", rset.getString("char_name"));
				hero.set("char_id", rset.getString("char_id"));
				hero.set("class_id", rset.getInt("class_id"));
				hero.set("count", rset.getInt("cnt"));
				hero.set("active", rset.getInt("active"));
				hero.set("message", rset.getString("message"));
				int clanId = rset.getInt("clanid");
				int allyId = rset.getInt("ally_id");

				if(clanId > 0)
				{

					hero.set("clan_name", ClanTable.getInstance().getClan(clanId).getName());
					hero.set("clan_crest", ClanTable.getInstance().getClan(clanId).getCrestId());

					if(allyId > 0)
					{
						hero.set("ally_name", ClanTable.getInstance().getClan(clanId).getAlliance().getAllyName());
						hero.set("ally_crest", ClanTable.getInstance().getClan(clanId).getAlliance().getAllyCrestId());
					}
					else
					{
						hero.set("ally_name", "");
						hero.set("ally_crest", 0);
					}
				}
				else
				{
					hero.set("clan_name", "");
					hero.set("clan_crest", 0);
				}

				_heroes.put(charId, hero);
			}

			rset.close();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error while loading hero data! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		_olyLog.info("Heros loaded: " + _heroes.size());
		loadHeroHistory();
		loadMatchHistory();
	}

	public static void setHeroMessage(int charId, String msg)
	{
		StatsSet hero = _heroes.get(charId);
		if(hero == null)
			return;
		if(msg == null || msg.length() == 0)
			return;
		if(msg.length() > 100)
			msg = msg.substring(0, 100);
		hero.set("message", msg);
		PreparedStatement stmt;

		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement("UPDATE hero_history SET message=? WHERE char_id=? and mons=?");
			stmt.setString(1, msg);
			stmt.setInt(2, charId);
			stmt.setInt(3, Integer.parseInt(Calendar.getInstance().get(Calendar.YEAR) + "" + (Calendar.getInstance().get(Calendar.MONTH) < 10 ? "0" + Calendar.getInstance().get(Calendar.MONTH) : Calendar.getInstance().get(Calendar.MONTH))));
			stmt.execute();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error while saving hero message! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void loadHeroHistory()
	{
		_olyLog.info("Loading hero history messages");
		_heroHistory = new FastMap<Integer, List<StatsSet>>();

		final String sql = "SELECT " +
				"  hh.class_id," +
				"  hl.descr," +
				"  hl.stdt " +
				"FROM " +
				"  hero_life_history hl" +
				"    inner join hero_history hh on(hh.char_id = hl.hero_id) " +
				"WHERE " +
				"  hh.mons = ? " +
				"ORDER BY hl.stdt ASC";

		PreparedStatement stmt;
		ResultSet rset;

		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, Integer.parseInt(Calendar.getInstance().get(Calendar.YEAR) + "" + (Calendar.getInstance().get(Calendar.MONTH) < 10 ? "0" + Calendar.getInstance().get(Calendar.MONTH) : Calendar.getInstance().get(Calendar.MONTH))));
			rset = stmt.executeQuery();

			while(rset.next())
			{
				List<StatsSet> msgs = _heroHistory.get(rset.getInt("class_id"));
				StatsSet hist = new StatsSet();
				hist.set("time", rset.getLong("stdt"));
				hist.set("message", rset.getString("descr"));
				if(msgs == null)
				{
					msgs = new FastList<StatsSet>();
					msgs.add(hist);
					_heroHistory.put(rset.getInt("class_id"), msgs);
				}
				else
					msgs.add(hist);
			}
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error while loading hero messages! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

	}

	public static boolean canBeAHero(int objectId)
	{
		StatsSet hero = _heroes.get(objectId);
		return hero != null && hero.getInteger("active") == 0;
	}

	public static void giveHeroBonuses(L2Player player)
	{
		if(player == null || _heroes == null)
			return;

		player.setHero(true);
		_heroes.get(player.getObjectId()).set("active", 1);
		Olympiad.getNoblesData(player).set("prev_points", Olympiad.getNoblesData(player).getInteger("prev_points", 0) + Config.ALT_OLY_HERO_POINTS_REWARD);
		L2Clan clan = player.getClan();
		if(clan != null && clan.getLevel() >= 5)
		{
			clan.incReputation(Config.ALT_OLY_HERO_CRP_REWARD, true, "Hero");
			clan.broadcastToOnlineMembers(new PledgeShowInfoUpdate(clan));
			clan.broadcastToOtherOnlineMembers(new SystemMessage(SystemMessage.CLAN_MEMBER_S1_WAS_NAMED_A_HERO_2S_POINTS_HAVE_BEEN_ADDED_TO_YOUR_CLAN_REPUTATION_SCORE).addString(player.getName()).addNumber((int)(Config.ALT_OLY_HERO_CRP_REWARD * Config.RATE_CLAN_REP_SCORE)), player);
		}
		player.broadcastUserInfo(true);
		updateHeroHistory(player.getObjectId(), new CustomMessage("OlympiadGainedHero", Config.DEFAULT_LANG).toString());

		if(!player.isSubClassActive())
		{
			player.addSkill(SkillTable.getInstance().getInfo(395, 1));
			player.addSkill(SkillTable.getInstance().getInfo(396, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1374, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1375, 1));
			player.addSkill(SkillTable.getInstance().getInfo(1376, 1));
			player.sendPacket(new SkillList(player));
		}

		final String sql = "UPDATE hero_history SET active = 1 WHERE mons=? and char_id=?";
		int mons = Integer.parseInt(String.format("%04d%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH)));
		PreparedStatement stmt;
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, mons);
			stmt.setInt(2, player.getObjectId());
			stmt.execute();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error updateing hero status " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static boolean isActiveHero(int charId)
	{
		if(_heroes == null || _heroes.size() == 0) return false;
		StatsSet hero = _heroes.get(charId);
		return hero != null && hero.getInteger("active") == 1;
	}

	public static void updateHeroHistory(int charId, String msg)
	{
		if(!isActiveHero(charId))
			return;
		if(msg == null)
			return;
		if(msg.length() > 100)
			msg = msg.substring(0, 100);

		final String sql = "INSERT INTO hero_life_history(hero_id,descr,stdt) VALUES(?,?,?)";
		long timeStamp = System.currentTimeMillis();

		StatsSet hist = new StatsSet();
		hist.set("time", timeStamp);
		hist.set("message", msg);

		List<StatsSet> msgs = _heroHistory.get(_heroes.get(charId).getInteger("class_id"));
		if(msgs == null)
		{
			msgs = new FastList<StatsSet>();
			msgs.add(hist);
			_heroHistory.put(_heroes.get(charId).getInteger("class_id"), msgs);
		}
		else
			msgs.add(hist);

		PreparedStatement stmt;
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, charId);
			stmt.setString(2, msg);
			stmt.setLong(3, timeStamp);
			stmt.execute();
			stmt.close();
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error updateing hero history! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void onViewHeroHistory(L2Player player, String bypass)
	{
		// _diary?class=109&page=1
		bypass = bypass.substring(13);
		StringTokenizer st = new StringTokenizer(bypass, "&");
		int classId = 0;
		int page = 0;
		try
		{
			classId = Integer.parseInt(st.nextToken());
			page = Integer.parseInt(st.nextToken().substring(5));
		}
		catch(Exception e)
		{
			_olyLog.warn("Error parsing bypass for vew history " + e + " " + bypass);
			return;
		}

		if(classId != 0 && page != 0)
		{
			List<StatsSet> msgs = _heroHistory.get(classId);
			StatsSet hero = getHeroInClass(classId);
			if(hero == null)
				return;
			NpcHtmlMessage reply = new NpcHtmlMessage(player, player.getLastNpc(), Olympiad.OLYMPIAD_HTML_FILE + "hero_history.htm", 1);
			reply.replace("%heroName%", hero.getString("char_name"));
			reply.replace("%message%", hero.getString("message"));

			TextBuilder html = new TextBuilder();
			int nPg = 0;
			if(msgs != null && msgs.size() > 0)
			{
				nPg = page + 1;
				int s = msgs.size();
				int end = s - page * 10;
				if(end < 0)
				{
					nPg = page;
					end = 0;
				}
				for(int start = s - (page - 1) * 10; start > end; start--)
				{
					if(msgs.get(start - 1) == null) continue;
					html.append("<font color=\"LEVEL\">" + getFormatedDate(msgs.get(start - 1).getLong("time")) + "</font><br1>");
					html.append(msgs.get(start - 1).getString("message") + "<br>");
				}
			}
			reply.replace("%history%", html.toString());
			player.sendPacket(reply);
		}

	}

	public static void onViewHeroMatchHistory(L2Player player, String bypass)
	{
		// _match?class=107&page=1
		bypass = bypass.substring(13);
		StringTokenizer st = new StringTokenizer(bypass, "&");
		int classId;
		int page;

		try
		{
			classId = Integer.parseInt(st.nextToken());
			page = Integer.parseInt(st.nextToken().substring(5));
		}
		catch(Exception e)
		{
			_olyLog.warn("Error parsing bypass for vew history " + e + " " + bypass);
			return;
		}

		if(classId != 0 && page != 0)
		{
			StatsSet hero = getHeroInClass(classId);
			if(hero == null)
				return;

			List<StatsSet> matches = _heroMatchHistory.get(hero.getInteger("char_id"));

			NpcHtmlMessage reply = new NpcHtmlMessage(player, player.getLastNpc(), Olympiad.OLYMPIAD_HTML_FILE + "match_history.htm", 1);

			int nPg = 0;

			if(matches != null && matches.size() > 0)
			{
				nPg = page + 1;
				int s = matches.size();
				int end = page * 15;

				if(end >= s)
				{
					nPg = page;
					end = s;
				}

				int start = (page - 1) * 15;

				int type = 0; int wins = 0;	int loos = 0; int draw = 0;

				TextBuilder html = new TextBuilder();
				for(int i=0; i < s; i++)
				{
					StatsSet match = matches.get(i);
					if(match == null)
						continue;
					String res = "";

					if(match.getInteger("result") == 1)
					{
						wins++;
						res = "   <font color=\"0000ff\">victory</font> "+wins+" victory "+draw+" draw "+loos+" loss<br>";
					}
					else if(match.getInteger("result") == 2)
					{
						loos++;
						res = "   <font color=\"ff0000\">loss</font> "+wins+" victory "+draw+" draw "+loos+" loss<br>";
					}
					else if(match.getInteger("result") == 3)
					{
						draw++;
						res = "   <font color=\"00ff00\">draw</font> "+wins+" victory "+draw+" draw "+loos+" loss<br>";
					}

					if(i >= start && i < end)
					{
						if(match.getInteger("type") != type)
						{
							type = match.getInteger("type");
							CustomMessage cm = new CustomMessage(type == 0 ? "OlympiadTeamClass" : type == 1 ? "OlympiadNonClass" : "OlympiadClass", Config.DEFAULT_LANG);
							html.append(cm.toString()+"<br>");
						}
						//<font color="LEVEL">2009year 01month 31day 21hour 10minute</font><br1>
						//vs Malda (<ClassID>112</ClassID>) (0minute 00second)   <font color="0000ff">victory</font>  1victory 0draw 0loss<br>
						html.append("<font color=\"LEVEL\">"+getFormatedDateMin(match.getLong("stdt"))+"</font><br1>");
						html.append("vs "+match.getString("vsName")+" (<ClassID>"+match.getInteger("vsClassId")+"</ClassID>) ("+getMatchTime(match.getInteger("time"))+")");
						html.append(res);
					}
				}
				reply.replace("%history%",html.toString());
				reply.replace("%totalScore%", new CustomMessage("OlympiadTotalScore", Config.DEFAULT_LANG).addNumber(wins).addNumber(draw).addNumber(loos).toString());
			}
			else
			{
				reply.replace("%history%", "");
				reply.replace("%totalScore%", new CustomMessage("OlympiadTotalScore", Config.DEFAULT_LANG).addNumber(0).addNumber(0).addNumber(0).toString());
			}

			if(page > 1)
				reply.replace("%prevBtn%", "<button value = \"&$1037;\" action=\"bypass _match?class=" + classId + "&page=" + (page - 1) +"\"  back=\"L2UI_CT1.Button_DF_Small_DOWN\" fore=\"L2UI_CT1.Button_DF_Small\" width=70 height=25  >");
			else
				reply.replace("%prevBtn%", "");
			if(nPg != page)
				reply.replace("%nextBtn%", "<button value = \"&$1038;\" action=\"bypass _match?class=" + classId + "&page=" + (page + 1) + "\"  back=\"L2UI_CT1.Button_DF_Small_DOWN\" fore=\"L2UI_CT1.Button_DF_Small\" width=70 height=25  >");
			else
				reply.replace("%nextBtn%", "");

			player.sendPacket(reply);
		}
	}

	public static void updateMatchHistory(OlympiadTeam ot1, OlympiadTeam ot2, int fightTime, int result)
	{
		final String sql = "INSERT INTO olymp_matches(char_id, vsName, vsClassId, result, type, mons, time, stdt) VALUES(?,?,?,?,?,?,?,?)";

		PreparedStatement stmt;
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for(OlympiadUserInfo oui : ot1.getPlayersInfo())
			{
				stmt = con.prepareStatement(sql);
				stmt.setInt(1, oui.getObjectId());
				stmt.setString(2, ot2.getName());
				stmt.setInt(3, ot2.getPlayersInfo().get(0).getClassId());
				stmt.setInt(4, result);
				stmt.setInt(5, ot1.getGameType());
				stmt.setInt(6, Integer.parseInt(String.format("%04d%02d", Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH))));
				stmt.setInt(7, fightTime);
				stmt.setLong(8, System.currentTimeMillis());
				stmt.execute();
				stmt.close();
			}
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error updateing match history! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	private static void loadMatchHistory()
	{
		final String sql = "SELECT " +
				"  om.* " +
				"FROM " +
				"  olymp_matches om " +
				"    inner join hero_history hh on(hh.char_id = om.char_id) " +
				"WHERE " +
				"  hh.mons = ? and " +
				"  om.mons = ? " +
				"ORDER by om.type DESC, om.stdt DESC";

		_heroMatchHistory = new FastMap<Integer, List<StatsSet>>();

		Calendar calend = Calendar.getInstance();
		int mons = Integer.parseInt(String.format("%04d%02d", calend.get(Calendar.YEAR), calend.get(Calendar.MONTH)));
		calend.set(Calendar.MONTH, calend.get(Calendar.MONTH)-1);
		int pMons = Integer.parseInt(String.format("%04d%02d", calend.get(Calendar.YEAR), calend.get(Calendar.MONTH)));
		_olyLog.info("Loading hero matches for: "+mons+"/"+pMons);

		PreparedStatement stmt;
		ResultSet rset;

		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			stmt = con.prepareStatement(sql);
			stmt.setInt(1, mons);
			stmt.setInt(2, pMons);
			rset = stmt.executeQuery();

			while(rset.next())
			{
				List<StatsSet> matches = _heroMatchHistory.get(rset.getInt("char_id"));
				StatsSet match = new StatsSet();
				match.set("char_id", rset.getInt("char_id"));
				match.set("vsName", rset.getString("vsName"));
				match.set("vsClassId", rset.getInt("vsClassId"));
				match.set("result", rset.getInt("result"));
				match.set("type", rset.getInt("type"));
				match.set("time", rset.getInt("time"));
				match.set("stdt", rset.getLong("stdt"));
				if(matches == null)
				{
					matches = new FastList<StatsSet>();
					matches.add(match);
					_heroMatchHistory.put(rset.getInt("char_id"), matches);
				}
				else
					matches.add(match);
			}
		}
		catch(SQLException e)
		{
			_olyLog.warn("Error while loading hero matches! " + e);
		}
		finally
		{
			try
			{
				con.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		_olyLog.info("Loading hero matches loaded.");
	}

	public static StatsSet getHeroInClass(int classId)
	{
		if(_heroes == null || _heroes.size() == 0) return null;
		for(int charId : _heroes.keySet())
		{
			if(_heroes.get(charId).getInteger("class_id") == classId) return _heroes.get(charId);
		}
		return null;
	}

	public static int[] getHeroItems()
	{
		return _heroItems;
	}

	public static String getFormatedDate(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		CustomMessage cm = new CustomMessage("OlympiadDateFormat", Config.DEFAULT_LANG);
		cm.addNumber(calendar.get(Calendar.YEAR));
		cm.addNumber(calendar.get(Calendar.MONTH) + 1);
		cm.addNumber(calendar.get(Calendar.DAY_OF_MONTH));
		cm.addNumber(calendar.get(Calendar.HOUR_OF_DAY));

		return cm.toString();
	}

	public static String getFormatedDateMin(long time)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		CustomMessage cm = new CustomMessage("OlympiadDateMinFormat", Config.DEFAULT_LANG);
		cm.addNumber(calendar.get(Calendar.YEAR));
		cm.addNumber(calendar.get(Calendar.MONTH) + 1);
		cm.addNumber(calendar.get(Calendar.DAY_OF_MONTH));
		cm.addNumber(calendar.get(Calendar.HOUR_OF_DAY));
		cm.addNumber(calendar.get(Calendar.MINUTE));

		return cm.toString();
	}

	public static String getMatchTime(int time)
	{
		int min = time / 60;
		int sec = time % 60;
		CustomMessage cm = new CustomMessage("OlympiadMatchTime", Config.DEFAULT_LANG);
		cm.addNumber(min);
		cm.addString(String.format("%02d", sec));
		return cm.toString();
	}

	public static void checkHeroForClanRemove(int objId)
	{
		if(_heroes.containsKey(objId))
		{
			StatsSet hero = _heroes.get(objId);
			hero.set("clan_name", "");
			hero.set("clan_crest", 0);
			hero.set("ally_name", "");
			hero.set("ally_crest", 0);
			_heroes.put(objId, hero);
		}
	}

	public static void checkHeroForClanAdd(int objId, L2Clan clan)
	{
		if(_heroes.containsKey(objId))
		{
			StatsSet hero = _heroes.get(objId);
			hero.set("clan_name", clan.getName());
			hero.set("clan_crest", clan.getCrestId());
			hero.set("ally_name", clan.getAlliance() == null ? "" : clan.getAlliance().getAllyName());
			hero.set("ally_crest", clan.getAlliance() == null ? 0 : clan.getAlliance().getAllyCrestId());
			_heroes.put(objId, hero);
		}
	}
}