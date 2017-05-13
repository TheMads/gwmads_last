package ru.l2gw.gameserver.model.instances;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeClan;
import ru.l2gw.gameserver.serverpackets.CastleSiegeInfo;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.io.File;
import java.util.StringTokenizer;

/**
 * Instance of Registrator to Ch siege
 * @author FlareDrakon L2f CCP
 */
public class L2CaptureTheFlagMessagerInstance extends L2SiegeMessengerInstance
{

	public L2CaptureTheFlagMessagerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	private static final Log _log = LogFactory.getLog(L2CaptureTheFlagMessagerInstance.class.getName());

	private boolean canBypass = false;

	private String standartCheck(final L2Player player, int val, final boolean onlyCl, final boolean onReg)
	{
		final int oldValue = val;
		if(player.getClanId() == 0)
			val = 25;
		else if(player.getClan().getLevel() < 4)
			val = 30;
		else if(!player.isClanLeader() && onlyCl)
			val = 26;
		else if(getBuilding(0).getSiege().isInProgress())
			val = 27;
		else if(getBuilding(0).getSiege().getTimeRemaining() > 2400000)//2 hours
			val = 28;
		else if(!onReg && !getBuilding(0).getSiege().checkIsAttacker(player.getClanId()))
			val = 29;
		if(oldValue == val)
			setCanBypass(true);
		else
			setCanBypass(false);
		return getHtmlPath(getNpcId(), val, 0);
	}

	@Override
	public String getHtmlPath(final int npcId, final int val, final int karma)
	{
		String pom;
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		final String temp = "data/html/chsiege/CaptureTheFlagMessager" + pom + ".htm";
		final File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}

	public void replaceClans(final NpcHtmlMessage html)
	{
		if(getBuilding(0) != null && !getBuilding(0).getSiege().isInProgress())
		{
			if(getBuilding(0).getSiege().getAttackerClans().isEmpty())
			{
				html.replace("%clan1%", "NoClan");
				html.replace("%clan2%", "NoClan");
				html.replace("%clan3%", "NoClan");
				html.replace("%clan4%", "NoClan");
				html.replace("%clan5%", "NoClan");
			}
			else
			{
				int count = 0;
				for(final SiegeClan SC : getBuilding(0).getSiege().getAttackerClans().values())
				{
					if(SC != null)
					{
						if(count == 0)
							html.replace("%clan1%", SC.getClan().getName());
						else if(count == 1)
							html.replace("%clan2%", SC.getClan().getName());
						else if(count == 2)
							html.replace("%clan3%", SC.getClan().getName());
						else if(count == 3)
							html.replace("%clan4%", SC.getClan().getName());
						else if(count == 4)
							html.replace("%clan5%", SC.getClan().getName());
						count++;
						if(count > 5)
						{
							_log.warn("We have six or more registred clans to CapchureTHe flag WTF?");
							continue;
						}
					}
				}
			}
		}
	}

	@Override
	public void showChatWindow(final L2Player player, final int val)
	{
		if(getBuilding(0) != null)
		{
			if(!getBuilding(0).getSiege().isInProgress())
				getHtmlPath(getNpcId(), 0, 0);
		}
		else
			player.sendPacket(new NpcHtmlMessage(player, this, "data/html/chsiege/busy.htm", val));
	}

	@Override
	public void onBypassFeedback(final L2Player player, final String command)
	{
		if(!isInRange(player, getInteractDistance(player)))
		{
			player.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, this);
			player.sendActionFailed();
		}
		else
			try
			{
				String file = null;
				if(command.startsWith("reg"))
				{
					final StringTokenizer st = new StringTokenizer(command.substring(0), " ");
					st.nextToken();
					final int how = Integer.valueOf(st.nextToken());
					file = standartCheck(player, 2, true, true);
					if(isCanBypass() && how == 3)
						file = getHtmlPath(getNpcId(), 3, 0);
					if(isCanBypass() && getBuilding(-1).getSiege().checkIsAttacker(player.getClanId()))
						file = getHtmlPath(getNpcId(), 31, 0);
				}
				if(command.equalsIgnoreCase("registerClan"))
				{
					file = standartCheck(player, 0, true, false);
					if(isCanBypass())
					{
						if(getBuilding(-1).getSiege().getAttackerClans().size() == 5 && !getBuilding(-1).getSiege().checkIsAttacker(player.getClanId()))
							file = getHtmlPath(getNpcId(), 13, 0);
						else if(getBuilding(-1).getSiege().checkIsAttacker(player.getClanId()))
							file = getHtmlPath(getNpcId(), 14, 0);
						else if(player.getClan().getMembersCount() < 18)
						{
							player.sendMessage("You must have 18 members to join this siege");//TODO: SM? CM ?
							return;
						}
					}
					else
					{
						player.sendPacket(new CastleSiegeInfo(getClanHall()));
						return;
					}
				}
				else if(command.equalsIgnoreCase("selectnpc"))
					file = standartCheck(player, 15, true, false);
				else if(command.startsWith("addNpc"))
				{
					file = standartCheck(player, 0, true, false);
					if(isCanBypass())
					{
						final StringTokenizer st = new StringTokenizer(command.substring(0), " ");
						st.nextToken();
						final int npcVar = Integer.valueOf(st.nextToken());
						if(getClanHall().getSiege().isNpcTaken(npcVar))
							player.sendMessage("This npc is already pick another clan select another");//TODO: CM ? SM?
						else
							getClanHall().getSiege().setNpc(player.getClanId(), npcVar, true);
					}
				}
				if(command.equalsIgnoreCase("registermember"))
				{
					file = standartCheck(player, 18, false, false);
					if(isCanBypass())
					{
						if(getClanHall().getSiege().isCountFull(player.getClanId()))
							file = getHtmlPath(getNpcId(), 16, 0);
						getBuilding(-1).getSiege().addMember(player, true);
						if(getClanHall().getSiege().getNpc(player.getClanId()) == 0)
							file = getHtmlPath(getNpcId(), 17, 0);
					}
				}
				else if(command.equalsIgnoreCase("viewnpc"))
				{
					final int npcvar = getClanHall().getSiege().getNpc(player.getClanId());

					if(npcvar > 5)
					{
						_log.warn("Clan: " + player.getClanId() + " Have npcid outofRange HTML bug users? Bun CL? ");
						return;
					}

					file = standartCheck(player, npcvar + 19, false, false);

					if(setCanBypass(true && getClanHall().getSiege().getNpc(player.getClanId()) == 0))
						file = getHtmlPath(getNpcId(), 19, 0);
				}

				final NpcHtmlMessage html = new NpcHtmlMessage(player, this);

				if(command.equalsIgnoreCase("ClanList"))
				{
					file = getHtmlPath(getNpcId(), 4, 0);
					html.setFile(file);
					replaceClans(html);
				}
				else
				{
					html.setFile(file);
					if(html.toString().contains("%siegeDate%"))
						html.replace("%siegeDate%", String.valueOf(getBuilding(-1).getSiege().getSiegeDate().getTimeInMillis() / 1000));
				}
				player.sendPacket(html);
			}
			catch(final StringIndexOutOfBoundsException sioobe)
			{
				_log.info("Incorrect htm bypass! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
			catch(final NumberFormatException nfe)
			{
				_log.info("Invalid bypass to Server command parameter! npcId=" + getTemplate().npcId + " command=[" + command + "]");
			}
	}

	public boolean setCanBypass(final boolean canBypass)
	{
		this.canBypass = canBypass;
		return canBypass;
	}

	public boolean isCanBypass()
	{
		return canBypass;
	}
}
