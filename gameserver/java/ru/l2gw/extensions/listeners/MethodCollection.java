package ru.l2gw.extensions.listeners;

/**
 * @author Death
 */
public interface MethodCollection
{
	public static final String ReduceCurrentHp = "L2Character.ReduceCurrentHp";
	public static final String L2ZoneObjectEnter = "L2Zone.onZoneEnter";
	public static final String L2ZoneObjectLeave = "L2Zone.onZoneLeave";
	public static final String L2ZoneChanged = "L2Zone.zoneChanged";
	public static final String AbstractAInotifyEvent = "AbstractAI.notifyEvent";
	public static final String AbstractAIsetIntention = "AbstractAI.setIntention";
	public static final String onStartAttack = "L2Character.doAttack";
	public static final String onStartCast = "L2Character.doCast";
	public static final String onStartAltCast = "L2Character.altUseSkill";
	public static final String onSkillUse = "RequestMagicSkillUse";
	public static final String onTradeStart = "AnswerTradeRequest";
	public static final String onAttacked = "L2Character.onHitTimer";
	public static final String onDecay = "L2Character.onDecay";
	public static final String doDie = "L2Character.doDie";
	public static final String onKill = "L2Character.doDie.KillerNotifier";
	public static final String onEffectAdd = "L2Skill.applyEffect";
	public static final String onActionRequest = "onActionRequest";
	public static final String onMoveRequest = "onMoveRequest";
	public static final String onDoorOpenClose = "onDoorOpenClose";
	public static final String onHeal = "onHeal";
}
