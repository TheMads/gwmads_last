package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.utils.StringUtil;
import ru.l2gw.gameserver.clientpackets.Say2C;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.L2GameServerPacket;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.Say2;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.GmListTable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class PetitionManager
{
	protected static Log _log = LogFactory.getLog(PetitionManager.class.getName());

	public static enum PetitionState
	{
		Pending,
		Responder_Cancel,
		Responder_Missing,
		Responder_Reject,
		Responder_Complete,
		Petitioner_Cancel,
		Petitioner_Missing,
		In_Process,
		Completed
	}

	public static enum PetitionType
	{
		Immobility,
		Recovery_Related,
		Bug_Report,
		Quest_Related,
		Bad_User,
		Suggestions,
		Game_Tip,
		Operation_Related,
		Other
	}

	private static final PetitionManager _instance = new PetitionManager();

	public static final PetitionManager getInstance()
	{
		return _instance;
	}

	private AtomicInteger _nextId = new AtomicInteger();
	private Map<Integer, Petition> _pendingPetitions = new ConcurrentHashMap<Integer, Petition>();
	private Map<Integer, Petition> _completedPetitions = new ConcurrentHashMap<Integer, Petition>();

	private class Petition
	{
		private long _submitTime = System.currentTimeMillis();
		private long _endTime = -1;
		private int _id;
		private PetitionType _type;
		private PetitionState _state = PetitionState.Pending;
		private String _content;
		private List<Say2> _messageLog = new ArrayList<Say2>();
		private int _petitioner;
		private int _responder;

		public Petition(L2Player petitioner, String petitionText, int petitionType)
		{
			_id = getNextId();
			_type = PetitionType.values()[petitionType - 1];
			_content = petitionText;
			_petitioner = petitioner.getObjectId();
		}

		protected boolean addLogMessage(Say2 cs)
		{
			return _messageLog.add(cs);
		}

		protected List<Say2> getLogMessages()
		{
			return _messageLog;
		}

		public boolean endPetitionConsultation(PetitionState endState)
		{
			setState(endState);
			_endTime = System.currentTimeMillis();
			if(getResponder() != null && getResponder().isOnline())
				if(endState == PetitionState.Responder_Reject)
					getPetitioner().sendMessage("Your petition was rejected. Please try again later.");
				else
				{
					getResponder().sendPacket(new SystemMessage(SystemMessage.ENDING_PETITION_CONSULTATION_WITH_S1).addString(getPetitioner().getName()));

					if(endState == PetitionState.Petitioner_Cancel)
						getResponder().sendPacket(new SystemMessage(SystemMessage.RECEIPT_NO_S1_PETITION_CANCELLED).addNumber(getId()));
				}

			if(getPetitioner() != null && getPetitioner().isOnline())
				getPetitioner().sendPacket(new SystemMessage(SystemMessage.ENDING_PETITION_CONSULTATION));

			getCompletedPetitions().put(getId(), this);
			return getPendingPetitions().remove(getId()) != null;
		}

		public String getContent()
		{
			return _content;
		}

		public int getId()
		{
			return _id;
		}

		public L2Player getPetitioner()
		{
			return L2ObjectsStorage.getPlayer(_petitioner);
		}

		public L2Player getResponder()
		{
			return L2ObjectsStorage.getPlayer(_responder);
		}

		@SuppressWarnings("unused")
		public long getEndTime()
		{
			return _endTime;
		}

		public long getSubmitTime()
		{
			return _submitTime;
		}

		public PetitionState getState()
		{
			return _state;
		}

		public String getTypeAsString()
		{
			return _type.toString().replace("_", " ");
		}

		public void sendPetitionerPacket(L2GameServerPacket responsePacket)
		{
			if(getPetitioner() == null || !getPetitioner().isOnline())
				return;

			getPetitioner().sendPacket(responsePacket);
		}

		public void sendResponderPacket(L2GameServerPacket responsePacket)
		{
			if(getResponder() == null || !getResponder().isOnline())
			{
				endPetitionConsultation(PetitionState.Responder_Missing);
				return;
			}

			getResponder().sendPacket(responsePacket);
		}

		public void setState(PetitionState state)
		{
			_state = state;
		}

		public void setResponder(L2Player responder)
		{
			if(getResponder() != null)
				return;

			_responder = responder.getObjectId();
		}
	}

	private PetitionManager()
	{
		_log.info("Initializing PetitionManager");
	}

	public int getNextId()
	{
		return _nextId.incrementAndGet();
	}

	public void clearCompletedPetitions()
	{
		int numPetitions = getPendingPetitionCount();
		getCompletedPetitions().clear();
		_log.info("PetitionManager: Completed petition data cleared. " + numPetitions + " petition(s) removed.");
	}

	public void clearPendingPetitions()
	{
		int numPetitions = getPendingPetitionCount();
		getPendingPetitions().clear();
		_log.info("PetitionManager: Pending petition queue cleared. " + numPetitions + " petition(s) removed.");
	}

	public boolean acceptPetition(L2Player respondingAdmin, int petitionId)
	{
		if(!isValidPetition(petitionId))
			return false;

		Petition currPetition = getPendingPetitions().get(petitionId);
		if(currPetition.getResponder() != null)
			return false;

		currPetition.setResponder(respondingAdmin);
		currPetition.setState(PetitionState.In_Process);
		currPetition.sendPetitionerPacket(new SystemMessage(SystemMessage.PETITION_APPLICATION_ACCEPTED));
		currPetition.sendResponderPacket(new SystemMessage(SystemMessage.PETITION_APPLICATION_ACCEPTED_RECEIPT_NO_IS_S1).addNumber(currPetition.getId()));
		currPetition.sendResponderPacket(new SystemMessage(SystemMessage.PETITION_CONSULTATION_WITH_S1_UNDER_WAY).addString(currPetition.getPetitioner().getName()));
		return true;
	}

	public boolean cancelActivePetition(L2Player player)
	{
		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Petitioner_Cancel);

			if(currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Responder_Cancel);
		}
		return false;
	}

	public void checkPetitionMessages(L2Player petitioner)
	{
		if(petitioner != null)
			for(Petition currPetition : getPendingPetitions().values())
			{
				if(currPetition == null)
					continue;

				if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId())
				{
					for(Say2 logMessage : currPetition.getLogMessages())
						petitioner.sendPacket(logMessage);
					return;
				}
			}
	}

	public boolean endActivePetition(L2Player player)
	{
		if(!player.isGM())
			return false;

		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition == null)
				continue;

			if(currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
				return currPetition.endPetitionConsultation(PetitionState.Completed);
		}
		return false;
	}

	protected Map<Integer, Petition> getCompletedPetitions()
	{
		return _completedPetitions;
	}

	protected Map<Integer, Petition> getPendingPetitions()
	{
		return _pendingPetitions;
	}

	public int getPendingPetitionCount()
	{
		return getPendingPetitions().size();
	}

	public int getPlayerTotalPetitionCount(L2Player player)
	{
		if(player == null)
			return 0;

		int petitionCount = 0;
		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition == null)
				continue;

			if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
				petitionCount++;
		}

		for(Petition currPetition : getCompletedPetitions().values())
		{
			if(currPetition == null)
				continue;

			if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
				petitionCount++;
		}
		return petitionCount;
	}

	public boolean isPetitionInProcess()
	{
		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition == null)
				continue;

			if(currPetition.getState() == PetitionState.In_Process)
				return true;
		}
		return false;
	}

	public boolean isPetitionInProcess(int petitionId)
	{
		if(!isValidPetition(petitionId))
			return false;

		Petition currPetition = getPendingPetitions().get(petitionId);
		return currPetition.getState() == PetitionState.In_Process;
	}

	public boolean isPlayerInConsultation(L2Player player)
	{
		if(player != null)
			for(Petition currPetition : getPendingPetitions().values())
			{
				if(currPetition == null)
					continue;

				if(currPetition.getState() != PetitionState.In_Process)
					continue;

				if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId() || currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
					return true;
			}

		return false;
	}

	public boolean isPetitioningAllowed()
	{
		return Config.PETITIONING_ALLOWED;
	}

	public boolean isPlayerPetitionPending(L2Player petitioner)
	{
		if(petitioner != null)
			for(Petition currPetition : getPendingPetitions().values())
			{
				if(currPetition == null)
					continue;

				if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == petitioner.getObjectId())
					return true;
			}

		return false;
	}

	private boolean isValidPetition(int petitionId)
	{
		return getPendingPetitions().containsKey(petitionId);
	}

	public boolean rejectPetition(L2Player respondingAdmin, int petitionId)
	{
		if(!isValidPetition(petitionId))
			return false;

		Petition currPetition = getPendingPetitions().get(petitionId);
		if(currPetition.getResponder() != null)
			return false;

		currPetition.setResponder(respondingAdmin);
		return currPetition.endPetitionConsultation(PetitionState.Responder_Reject);
	}

	public boolean sendActivePetitionMessage(L2Player player, String messageText)
	{
		Say2 cs;
		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition == null)
				continue;

			if(currPetition.getPetitioner() != null && currPetition.getPetitioner().getObjectId() == player.getObjectId())
			{
				cs = new Say2(player.getObjectId(), Say2C.PETITION_PLAYER, player.getName(), messageText);
				currPetition.addLogMessage(cs);

				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				return true;
			}

			if(currPetition.getResponder() != null && currPetition.getResponder().getObjectId() == player.getObjectId())
			{
				cs = new Say2(player.getObjectId(), Say2C.PETITION_GM, player.getName(), messageText);
				currPetition.addLogMessage(cs);

				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				return true;
			}
		}
		return false;
	}

	public void sendPendingPetitionList(L2Player activeChar)
	{
		final StringBuilder htmlContent = StringUtil.startAppend(600 + getPendingPetitionCount() * 300, "<html><body><center><table width=270><tr>" + "<td width=45><button value=\"Main\" action=\"bypass -h admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td width=180><center>Petition Menu</center></td>" + "<td width=45><button value=\"Back\" action=\"bypass -h admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br>" + "<table width=\"270\">" + "<tr><td><table width=\"270\"><tr><td><button value=\"Reset\" action=\"bypass -h admin_reset_petitions\" width=\"80\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td align=right><button value=\"Refresh\" action=\"bypass -h admin_view_petitions\" width=\"80\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br></td></tr>");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		if(getPendingPetitionCount() == 0)
			htmlContent.append("<tr><td>There are no currently pending petitions.</td></tr>");
		else
			htmlContent.append("<tr><td><font color=\"LEVEL\">Current Petitions:</font><br></td></tr>");

		boolean color = true;
		int petcount = 0;
		for(Petition currPetition : getPendingPetitions().values())
		{
			if(currPetition == null)
				continue;

			StringUtil.append(htmlContent, "<tr><td width=\"270\"><table width=\"270\" cellpadding=\"2\" bgcolor=", (color ? "131210" : "444444"), "><tr><td width=\"130\">", dateFormat.format(new Date(currPetition.getSubmitTime())));
			StringUtil.append(htmlContent, "</td><td width=\"140\" align=right><font color=\"", (currPetition.getPetitioner().isOnline() ? "00FF00" : "999999"), "\">", currPetition.getPetitioner().getName(), "</font></td></tr>");
			StringUtil.append(htmlContent, "<tr><td width=\"130\">");
			if(currPetition.getState() != PetitionState.In_Process)
				StringUtil.append(htmlContent, "<table width=\"130\" cellpadding=\"2\"><tr>" + "<td><button value=\"View\" action=\"bypass -h admin_view_petition ", String.valueOf(currPetition.getId()), "\" width=\"50\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>" + "<td><button value=\"Reject\" action=\"bypass -h admin_reject_petition ", String.valueOf(currPetition.getId()), "\" width=\"50\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			else
				htmlContent.append("<font color=\"" + (currPetition.getResponder().isOnline() ? "00FF00" : "999999") + "\">" + currPetition.getResponder().getName() + "</font>");
			StringUtil.append(htmlContent, "</td>", currPetition.getTypeAsString(), "<td width=\"140\" align=right>", currPetition.getTypeAsString(), "</td></tr></table></td></tr>");
			color = !color;
			petcount++;
			if(petcount > 10)
			{
				htmlContent.append("<tr><td><font color=\"LEVEL\">There is more pending petition...</font><br></td></tr>");
				break;
			}
		}
		htmlContent.append("</table></center></body></html>");
		NpcHtmlMessage htmlMsg = new NpcHtmlMessage(0);
		htmlMsg.setHtml(htmlContent.toString());
		activeChar.sendPacket(htmlMsg);
	}

	public int submitPetition(L2Player petitioner, String petitionText, int petitionType)
	{
		Petition newPetition = new Petition(petitioner, petitionText, petitionType);
		int newPetitionId = newPetition.getId();
		getPendingPetitions().put(newPetitionId, newPetition);
		String msgContent = petitioner.getName() + " has submitted a new petition."; //(ID: " + newPetitionId + ").";
		GmListTable.broadcastToGMs(new Say2(petitioner.getObjectId(), Say2C.HERO_VOICE, "Petition System", msgContent));
		return newPetitionId;
	}

	public void viewPetition(L2Player activeChar, int petitionId)
	{
		if(!activeChar.isGM())
			return;

		if(!isValidPetition(petitionId))
			return;

		Petition currPetition = getPendingPetitions().get(petitionId);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/admin/petition.htm");
		html.replace("%petition%", String.valueOf(currPetition.getId()));
		html.replace("%time%", dateFormat.format(new Date(currPetition.getSubmitTime())));
		html.replace("%type%", currPetition.getTypeAsString());
		html.replace("%petitioner%", currPetition.getPetitioner().getName());
		html.replace("%online%", (currPetition.getPetitioner().isOnline() ? "00FF00" : "999999"));
		html.replace("%text%", currPetition.getContent());
		activeChar.sendPacket(html);
	}
}
