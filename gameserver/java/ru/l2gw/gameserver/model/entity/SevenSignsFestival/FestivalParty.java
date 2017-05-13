package ru.l2gw.gameserver.model.entity.SevenSignsFestival;

import javolution.util.FastList;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSigns;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.List;
import java.util.StringTokenizer;

/**
 * @author rage
 * Date: 04.06.2009 18:19:52
 */
public class FestivalParty
{
	private List<StatsSet> _members;
	private int _leader;
	private int _cabal;
	private long _validTime;
	private long _dateTime;
	private boolean _isAborted = false;
	private long _score;
	private int _festivalLevel;

	public FestivalParty(L2Party party)
	{
		_leader = party.getPartyLeader().getObjectId();
		_cabal = SevenSigns.getInstance().getPlayerCabal(party.getPartyLeader());
		_members = new FastList<StatsSet>();
		_validTime = System.currentTimeMillis() + 40 * 60000;
		_dateTime = System.currentTimeMillis();
		for(L2Player member : party.getPartyMembers())
		{
			if(member == null)
				continue;
			StatsSet mem = new StatsSet();
			mem.set("objectId", member.getObjectId());
			mem.set("name", member.getName());
			mem.set("bonus", false);
			_members.add(mem);
		}
	}

	public FestivalParty(String partyInfo)
	{
		// String format: _leader;_cabal;_festivalLevel;_dateTime;_isAborted;_score;name,objectId,bonusResived;name,objectId...
		StringTokenizer st = new StringTokenizer(partyInfo, ";");
		_leader = Integer.parseInt(st.nextToken());
		_cabal = Integer.parseInt(st.nextToken());
		_festivalLevel = Integer.parseInt(st.nextToken()); 
		_dateTime = Long.parseLong(st.nextToken());
		_isAborted = Boolean.parseBoolean(st.nextToken());
		_score = Integer.parseInt(st.nextToken());
		_members = new FastList<StatsSet>();

		while(st.hasMoreTokens())
		{
			String[] info = st.nextToken().split(",");

			if(info.length == 3)
			{
				StatsSet mem = new StatsSet();
				mem.set("name", info[0]);
				mem.set("objectId", Integer.parseInt(info[1]));
				mem.set("bonus", Boolean.parseBoolean(info[2]));
				_members.add(mem);
			}
		}
	}

	public int getPartyLeaderObjId()
	{
		return _leader;
	}

	public long getValidTime()
	{
		return _validTime;
	}

	public void setAborted(boolean isAborted)
	{
		_isAborted = isAborted;
	}

	public boolean isAborted()
	{
		return _isAborted;
	}

	public void setFestivalLevel(int level)
	{
		_festivalLevel = level;
	}

	public int getFestivalLevel()
	{
		return _festivalLevel;
	}

	public void setScore(long score)
	{
		_score = score;
	}

	public long getScore()
	{
		return _score;
	}

	public long getDate()
	{
		return _dateTime;
	}

	public int getCabal()
	{
		return _cabal;
	}

	public List<StatsSet> getMembers()
	{
		return _members;
	}

	public StatsSet getMember(int objectId)
	{
		for(StatsSet member : _members)
			if(member.getInteger("objectId") == objectId)
				return member;

		return null;
	}

	public String getMembersString()
	{
		String members = "";
		int c = 1;
		for(StatsSet member : _members)
		{
			if(!members.isEmpty())
				members += c % 3 == 0 && _members.size() != c ? ", " + member.getString("name") + "<br1>" : ", " + member.getString("name");
			else
				members = member.getString("name");
			c++;
		}
		members += "<br>";

		return members;
	}

	@Override
	public String toString()
	{
		// String format: _leader;_cabal;_festivalLevel;_dateTime;_isAborted;_score;name,objectId,bonusResived;name,objectId...
		String ret = _leader + ";" + _cabal + ";" + _festivalLevel + ";" + _dateTime + ";" + _isAborted + ";" + _score + ";";
		for(StatsSet member : _members)
			ret += member.getString("name") + "," + member.getInteger("objectId") + "," + member.getBool("bonus") + ";";

		return ret;
	}
}
