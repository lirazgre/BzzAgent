package bzzAgent;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bzzagent.ObjectFactory;
import com.bzzagent.apps.core.model.JournalItem;
import com.bzzagent.apps.core.model.ModerationConfig;
import com.bzzagent.bo.CampaignInviteStatus;
import com.bzzagent.bzzactivity.BzzActivityBO.Status;
import com.bzzagent.bzzactivity.BzzActivityType;
import com.bzzagent.bzzperks.bo.BzzPerkRewardBO;
import com.bzzagent.common.domain.ActivityIncentiveEntity;
import com.bzzagent.common.domain.BzzActivityEntity;
import com.bzzagent.common.domain.CampaignEntity;
import com.bzzagent.common.domain.CampaignInviteEntity;
import com.bzzagent.common.domain.CampaignPermissionsEntity;
import com.bzzagent.common.domain.CampaignProfileEntity;
import com.bzzagent.common.domain.EcommerceEntity;
import com.bzzagent.common.enums.BsbActivityStatus;
import com.bzzagent.common.enums.CampaignType;
import com.bzzagent.common.service.ServiceAbstract;
import com.bzzagent.common.service.dao.BiteSizedBzzDao;
import com.bzzagent.common.service.dao.BiteSizedBzzDaoJpa;
import com.bzzagent.common.service.dao.BzzActivityDao;
import com.bzzagent.common.service.dao.BzzActivityDaoJpa;
import com.bzzagent.common.service.dao.CampaignDao;
import com.bzzagent.common.service.dao.CampaignDaoJpa;
import com.bzzagent.common.service.dao.EcommerceDao;
import com.bzzagent.common.service.dao.EcommerceDaoJpa;
import com.bzzagent.common.service.dao.MemberGroupDao;
import com.bzzagent.common.service.dao.MemberGroupDaoJpa;
import com.bzzagent.domain.BiteSizedActivityEntity;
import com.bzzagent.domain.BsbAgentActivityEntity;
import com.bzzagent.domain.MemberGroupEntity;
import com.bzzagent.event.EventBO;
import com.bzzagent.event.EventType;
import com.bzzagent.manager.bzzperks.BzzPerksManager;
import com.bzzagent.manager.event.EventManager;
import com.bzzagent.product.ProductType;
import com.bzzagent.util.Platform;
import com.bzzagent.util.StringUtils;
import com.bzzagent.webapp.beans.CampaignBean.Phase;
import com.bzzagent.webapp.beans.CampaignProfileBean.ProfileType;
import com.bzzagent.webapp.beans.CampaignScopeBean;
import com.bzzagent.webapp.beans.GenericSQLBean;
import com.bzzagent.webapp.util.BzzUtil;
import com.google.common.collect.Sets;

/**
 * 
 * @author lirazg
 *
 */
public class BiteSizedBzzServiceImpl extends ServiceAbstract implements BiteSizedBzzService {
    
    private static final Logger LOG = LogManager.getLogger();

    
	private BiteSizedBzzDao	biteSizedBzzDao;
	private MemberGroupDao	memberGroupDao;
	private BzzActivityDao	bzzActivityDao;
	private CampaignDao 	campaignDao;
	private EcommerceDao	ecommerceDao;
	private EventManager	eventManager;
	private BzzPerksManager	bzzPerksManager;
	
	public BiteSizedBzzServiceImpl() {
        this(null, null, null, null, null, ObjectFactory.get(EventManager.class), ObjectFactory.get(BzzPerksManager.class));
    }
    
    public BiteSizedBzzServiceImpl(BiteSizedBzzDao biteSizedBzzDao, BzzActivityDao  bzzActivityDao, MemberGroupDao	memberGroupDao, CampaignDao campaignDao,  EcommerceDao	ecommerceDao,
            EventManager eventManager, BzzPerksManager bzzPerksManager) {
		this.biteSizedBzzDao = biteSizedBzzDao;
		this.bzzActivityDao = bzzActivityDao;
		this.memberGroupDao = memberGroupDao;
		this.campaignDao = campaignDao;
		this.ecommerceDao = ecommerceDao;
		this.eventManager = eventManager;
		this.bzzPerksManager= bzzPerksManager;
	}
    
    private BzzActivityDao getBzzActivityDao() {
        if (bzzActivityDao == null) {
            // do not store the jpa implementation in the member attribute. we
            // need to recreate each time using the entity manager.
            final EntityManager em = create();
            return new BzzActivityDaoJpa(em);
        }
        return bzzActivityDao;
    }

    private EcommerceDao getEcommerceDao() {
        if (ecommerceDao == null) {
            // do not store the jpa implementation in the member attribute. we
            // need to recreate each time using the entity manager.
            final EntityManager em = create();
            return new EcommerceDaoJpa(em);
        }
        return ecommerceDao;
    }
    private CampaignDao getCampaignDao() {
        if (campaignDao == null) {
            // do not store the jpa implementation in the member attribute. we
            // need to recreate each time using the entity manager.
            final EntityManager em = create();
            return new CampaignDaoJpa(em);
        }
        return campaignDao;
    }

    private BiteSizedBzzDao getBiteSizedBzzDao() {
        if (biteSizedBzzDao == null) {
            // do not store the jpa implementation in the member attribute. we
            // need to recreate each time using the entity manager.
            final EntityManager em = create();
            return new BiteSizedBzzDaoJpa(em);
        }
        return biteSizedBzzDao;
    }

    private MemberGroupDao getMemberGroupDao() {
        if (memberGroupDao == null) {
            // do not store the jpa implementation in the member attribute. we
            // need to recreate each time using the entity manager.
            final EntityManager em = create();
            return new MemberGroupDaoJpa(em);
        }
        return memberGroupDao;
    }
    
 
    @Override
    public CreateBiteSizedActivityResponse create(BiteSizedActivityRequest request) {
        final BiteSizedActivityEntity biteSizedActivityEntity = request.getBsbActivity(); 
    	//validate the request
    	final String biteSizedActivityTitle = biteSizedActivityEntity.getTitle();
        Validate.notNull(biteSizedActivityTitle);
        
        //create the campaign.
        final CampaignEntity campaign =   setCampaign(biteSizedActivityEntity,new CampaignEntity());

        //create xref_campaign_permissions
        boolean isGalleryViewEnabled = biteSizedActivityEntity.isGalleryViewEnabled() != null &&
                biteSizedActivityEntity.isGalleryViewEnabled();

		CampaignPermissionsEntity campaignPermissionsEntity = new CampaignPermissionsEntity();
		campaignPermissionsEntity.setCampaignEntity(campaign);
		campaignPermissionsEntity.setPublic(false);
		campaignPermissionsEntity.setHistoric(false);
		campaignPermissionsEntity.setOnFacebook(false);
		campaignPermissionsEntity.setLastModified(new Timestamp(System.currentTimeMillis()));
		campaignPermissionsEntity.setLastModifiedBy(StringUtils.safeToInt(request.getMemberId(), 0));
		campaignPermissionsEntity.setVisible(isGalleryViewEnabled);
        campaignPermissionsEntity.setPublic(isGalleryViewEnabled);
        campaignPermissionsEntity.setGalleryVisibility(isGalleryViewEnabled ? CampaignScopeBean.GalleryVisibility.Public : CampaignScopeBean.GalleryVisibility.Hidden);
		campaign.setCampaignPermissionsEntity(campaignPermissionsEntity);
		
		//create the BzzActivity
 		final BzzActivityEntity activity = setActivity(biteSizedActivityEntity,new BzzActivityEntity(),campaign);
 		
 		final ModerationConfig moderationConfig = biteSizedActivityEntity.getModerationConfig();
		activity.setModerationLimit(moderationConfig.getLimit());
 		activity.setModerationQualities(moderationConfig.getLimitComdevQualitiesString());
 		activity.setInstructions(moderationConfig.getInstructions());
 		activity.setIsModerated(moderationConfig.isModerationEnabled());
 		
        final EntityManager em = create();
        try {
            
 		Set<EcommerceEntity> ecommerceRetailers = null;
 		//create Ecommerce
		if (activity.getBzzActivityType().equals(BzzActivityType.Ecommerce)) {
			
			ecommerceRetailers = biteSizedActivityEntity.getEcommerceRetailers();
			if (BzzUtil.isNotEmpty(ecommerceRetailers)) {
				//validate every ecommerce 
				for(EcommerceEntity ecom : ecommerceRetailers){
					EcommerceEntity ecommerceEntity = null;
					boolean isValid = true;
					if (StringUtils.isBlank(ecom.getIdent())) {
	                    isValid = false;
	                }else{
	                	ecommerceEntity = getEcommerceDao().findByIdent(ecom.getIdent());
						if (ecommerceEntity == null) {
	                        isValid = false;
	                    }
	                }
					if (!isValid) {
						return new CreateBiteSizedActivityResponse.Builder()
						.withEcommerceRetailNotFound()
						.build();
					}
	                activity.addEcommerceRetailer(ecommerceEntity);
				}
			} 
		}
 		addJournalEntry(request, activity);
		
 		//create ActivityIncentiveEntity 
 		final List<ActivityIncentiveEntity> incentives = biteSizedActivityEntity.getIncentives();
 		if (incentives != null) {
     		for(ActivityIncentiveEntity incentive : incentives){
				boolean isValid = true;
                if (StringUtils.isBlank(incentive.getBzzperkRewardIdent())) {
                    isValid = false;
                } else {
                    BzzPerkRewardBO reward = this.bzzPerksManager.findBzzPerkRewardByIdent(incentive.getBzzperkRewardIdent());
                    if (reward == null) {
                        isValid = false;
                    }
                }

				if (!isValid) {
					return new CreateBiteSizedActivityResponse.Builder()
					.withBzzPerkRewardNotFound()
					.build();
				}
     		}
            activity.setIncentives(incentives);
 		}
 		
 		// initialize the membergroup entity
 		MemberGroupEntity groupEntity = getMemberGroupDao().getById(biteSizedActivityEntity.getGroupID());
		if (groupEntity == null) {
			return new CreateBiteSizedActivityResponse.Builder()
				.withGroupNotFound()
				.build();
		} else if (!groupEntity.isBiteSizedBzzEnabled()) {
			return new CreateBiteSizedActivityResponse.Builder()
				.withGroupNotBiteSizedEnabled()
				.build();
		}

		// create campaign profile
		final CampaignProfileEntity campaignProfile = new CampaignProfileEntity();
		campaignProfile.setCampaignEntity(campaign);
        campaignProfile.setStartDate(biteSizedActivityEntity.getStartDate());
        campaignProfile.setEndDate(biteSizedActivityEntity.getEndDate());
		campaignProfile.setNumberOrAgents(Integer.MAX_VALUE);
		campaignProfile.setProfileType(ProfileType.Regular);
		campaignProfile.setMemberGroupEntity(groupEntity);
		campaign.addCampaignProfiles(campaignProfile);
		
 		//save everything
    		em.getTransaction().begin();
            getBzzActivityDao().create(activity);
    		em.getTransaction().commit();
 
            if (biteSizedActivityEntity.isLaunched()) {
            	this.handleCampaignInvites(activity);
            }
		
    	//build the return entity
    	final BiteSizedActivityEntity returnEntity =  new BiteSizedActivityEntity.Builder()
    	    .withBzzActivityEntity(activity)
    	    .withActivityIdent(activity.getIdent())
    	    .withCampaign(campaign)
    	    .withCampaignPermissions(campaignPermissionsEntity)
    	    .withGroupId(groupEntity.getId())
    	    .withActivityIncentiveEntity(incentives)
    	    .withEcommerceRetailersEntity(ecommerceRetailers)
            .withViewedCount(0)
            .withDismissedCount(0)
            .withCompletedCount(0)
    	    .build();
    	
    	//build response
    	return	new CreateBiteSizedActivityResponse.Builder()
            .withCreatedActivity(returnEntity)
            .build();  	
        } catch (RuntimeException e) {
            LOG.error("error creating bsb", e);
            throw e;
        } finally {
            close();
        }
    	
    }

    /**
     * Adds a {@link JournalItem} to the {@link BzzActivityEntity} with the current date, and the request's username and phase.
     */
    private void addJournalEntry(BiteSizedActivityRequest request, BzzActivityEntity activity) {
        final JournalItem journalItem = new JournalItem();
        journalItem.setTimestamp(new Date());
        journalItem.setStatus(request.getBsbActivity().getPhase().toString());
        journalItem.setUsername(request.getUsername());
        activity.addJournalItem(journalItem);
    }
    
    @Override
    public BiteSizedActivityResponse update(BiteSizedActivityRequest request) {
        final BiteSizedActivityEntity biteSizedActivityEntity = request.getBsbActivity();
    	//validate the request
    	Validate.notNull(biteSizedActivityEntity.getActivityIdent());
        final BiteSizedActivityEntity bsb;
        try {
            final EntityManager em = create();
            try {
                bsb = getBiteSizedBzzDao().getByIdent(biteSizedActivityEntity.getActivityIdent());
            } catch (NoResultException e) {
                return new BiteSizedActivityResponse.Builder()
                    .withActivityNotFound()
                    .build();
            }
		
            //get bzzactivity
    		BzzActivityEntity activity = bsb.getBzzActivityEntity();
    		
    		// some very simple BSB workflow
    		CampaignEntity campaign = activity.getCampaignEntity();
    		final Phase beforePhase = campaign.getPhase();
            switch (beforePhase) {
    			case Stub:
    				if (! Sets.immutableEnumSet(Phase.Stub, Phase.Prelaunch).contains(biteSizedActivityEntity.getPhase()) ) {
    					return new BiteSizedActivityResponse.Builder()
    		 		       .withInvalidPhase()
    		 		       .build();
    				}
    				break;
            
    			case Prelaunch:
    				if (! Sets.immutableEnumSet(Phase.Prelaunch, Phase.Launch).contains(biteSizedActivityEntity.getPhase()) ) {
    					return new BiteSizedActivityResponse.Builder()
    		 		       .withInvalidPhase()
    		 		       .build();
    				}
    				break;
    			
    			case Launch:
    				if (! Sets.immutableEnumSet(Phase.Launch, Phase.End).contains(biteSizedActivityEntity.getPhase()) ) {
    					return new BiteSizedActivityResponse.Builder()
    		 		       .withInvalidPhase()
    		 		       .build();
    				}
    				break;
    				
    			case End:
    				if (! Sets.immutableEnumSet(Phase.End).contains(biteSizedActivityEntity.getPhase()) ) {
    					return new BiteSizedActivityResponse.Builder()
    		 		       .withInvalidPhase()
    		 		       .build();
    				}
    				break;
    			
    			default:
    				break;
    		}
    		
            // update the campaign.
            campaign =   setCampaign(biteSizedActivityEntity,campaign);
    
        	if (beforePhase != campaign.getPhase()) {
        	    addJournalEntry(request, activity);
        	}
        	
        	// update xref_campaign_permissions
        	CampaignPermissionsEntity campaignPermissionsEntity = campaign.getCampaignPermissionsEntity();
    		campaignPermissionsEntity.setLastModified(new Timestamp(System.currentTimeMillis()));
    		campaignPermissionsEntity.setLastModifiedBy(StringUtils.safeToInt(request.getMemberId(),0));
    		campaignPermissionsEntity.setVisible(biteSizedActivityEntity.isGalleryViewEnabled().booleanValue());
    		campaign.setCampaignPermissionsEntity(campaignPermissionsEntity);
    	
    		// update the BzzActivity
    		em.getTransaction().begin();
     		if (biteSizedActivityEntity.getBzzActivityType().equals(BzzActivityType.Ecommerce)){
     			//clear current ecommerce before adding new ones
				getBzzActivityDao().clearActivityEcommerce(activity.getIdent());
     		}
     		activity = setActivity(biteSizedActivityEntity,activity, campaign);
     	
    		// do not allow user to change the ModerationConfig after the BSB has been launched
 			final ModerationConfig existingModerationConfig = bsb.getModerationConfig();
    		final ModerationConfig updatedModerationConfig = biteSizedActivityEntity.getModerationConfig();
            if ( !existingModerationConfig.equals(updatedModerationConfig) && biteSizedActivityEntity.isLaunched() ) {
                return new BiteSizedActivityResponse.Builder()
    	            .withInvalidPhase()
    	            .build();
            }
            else if( !biteSizedActivityEntity.isLaunched() ){
     			final ModerationConfig moderationConfig = biteSizedActivityEntity.getModerationConfig();
     			activity.setModerationLimit(moderationConfig.getLimit());
     	 		activity.setModerationQualities(moderationConfig.getLimitComdevQualitiesString());
     	 		activity.setInstructions(moderationConfig.getInstructions());
     	 		activity.setIsModerated(moderationConfig.isModerationEnabled());
     		}
    		
    		//create Ecommerce
     		Set<EcommerceEntity> ecommerceRetailers = null;
			if (activity.getBzzActivityType().equals(BzzActivityType.Ecommerce)) {
				
				ecommerceRetailers = biteSizedActivityEntity.getEcommerceRetailers();
				if (BzzUtil.isNotEmpty(ecommerceRetailers)) {
					//validate every ecommerce 
					for(EcommerceEntity ecom : ecommerceRetailers){
						EcommerceEntity ecommerceEntity = null;
						boolean isValid = true;
						if (StringUtils.isBlank(ecom.getIdent())) {
		                    isValid = false;
		                }else{
		                	ecommerceEntity = getEcommerceDao().findByIdent(ecom.getIdent());
							if (ecommerceEntity == null) {
		                        isValid = false;
		                    }
		                }
						if (!isValid) {
							return new BiteSizedActivityResponse.Builder()
							.withEcommerceRetailNotFound()
							.build();
						}
		                activity.addEcommerceRetailer(ecommerceEntity);
					}
				} 
			}
			
     		//create ActivityIncentiveEntity 
     		final List<ActivityIncentiveEntity> incentives = biteSizedActivityEntity.getIncentives();
     		
     		if (incentives != null) {
         		for(ActivityIncentiveEntity incentive : incentives){
    				BzzPerkRewardBO reward = this.bzzPerksManager.findBzzPerkRewardByIdent(incentive.getBzzperkRewardIdent());
    				if (reward == null) {
    					return new BiteSizedActivityResponse.Builder()
    					.withBzzPerkRewardNotFound()
    					.build();
    				}
         		}
         		activity.setIncentives(incentives);
     		}
     		
    		// get campaign profile
    		Set<CampaignProfileEntity> profiles = campaign.getCampaignProfiles();
    		if (BzzUtil.isEmpty(profiles)) {
    			return new BiteSizedActivityResponse.Builder()
    				.withMissingProfile()
    				.build();
    		} else if (profiles.size() > 1) {
    			return new BiteSizedActivityResponse.Builder()
    				.withMoreThanOneProfile()
    				.build();
    		}
    		CampaignProfileEntity campaignProfile = profiles.iterator().next();
    		
    		// do not allow user to change the group after the BSB has been launched
    		final int existingGroupId = campaignProfile.getMemberGroupEntity().getId();
    		final int updatedGroupId = biteSizedActivityEntity.getGroupID();
            if ( (existingGroupId != updatedGroupId) && biteSizedActivityEntity.isLaunched() ) {
                return new BiteSizedActivityResponse.Builder()
    	            .withInvalidPhase()
    	            .build();
            }
            
    		MemberGroupEntity groupEntity = getMemberGroupDao().getById(updatedGroupId);
    		if (groupEntity == null) {
    			return new BiteSizedActivityResponse.Builder()
    				.withGroupNotFound()
    				.build();
    		}
    		if (!groupEntity.isBiteSizedBzzEnabled()) {
    			return new BiteSizedActivityResponse.Builder()
    				.withGroupNotBiteSizedEnabled()
    				.build();
    		}

            campaignProfile.setStartDate(biteSizedActivityEntity.getStartDate());
    		campaignProfile.setEndDate(biteSizedActivityEntity.getEndDate());
    		campaignProfile.setMemberGroupEntity(groupEntity);
     		
     		// save everything
     		getBzzActivityDao().update(activity);
            em.getTransaction().commit();
            
            if (biteSizedActivityEntity.isLaunched()) {
            	this.handleCampaignInvites(activity);
            }
            
            //build the return entity
            final BiteSizedActivityEntity returnEntity =  new BiteSizedActivityEntity.Builder()
                .withBzzActivityEntity(activity)
                .withCampaign(campaign)
                .withCampaignPermissions(campaignPermissionsEntity)
                .withGroupId(updatedGroupId)
                .withActivityIncentiveEntity(incentives)
                .withEcommerceRetailersEntity(ecommerceRetailers)
                .withViewedCount(bsb.getViewedCount())
                .withDismissedCount(bsb.getDismissedCount())
                .withCompletedCount(bsb.getCompletedCount())
                .build();
            
            //build response
            return  new BiteSizedActivityResponse.Builder()
                .withCreatedActivity(returnEntity)
                .build();   
    	} finally {
			close();
		}
    	
    }

    private void handleCampaignInvites(BzzActivityEntity activity) {
        LOG.info("triggering invites for bsb : " + activity);
		EventBO launchEvent = new EventBO()
			.setType(EventType.BSBLaunch)
			.setObjectId((long) activity.getId())
			.setMemberId(0)
			.setPlatform(Platform.Undefined);
	
        eventManager.handle(launchEvent);
	}

    private CampaignEntity setCampaign(BiteSizedActivityEntity biteSizedActivityEntity, CampaignEntity campaign){
    	final String biteSizedActivityTitle = biteSizedActivityEntity.getTitle();
        Validate.notNull(biteSizedActivityTitle);
        
        campaign.setType(CampaignType.BSB);
        campaign.setPlainTextTitle(biteSizedActivityTitle);
        campaign.setName(biteSizedActivityTitle);
        campaign.setNickName(biteSizedActivityTitle);
        campaign.setAutoLaunchEnabled(true);
        campaign.setAutoLaunchDate(biteSizedActivityEntity.getStartDate());
        campaign.setStartDate(biteSizedActivityEntity.getStartDate());
        campaign.setEndDate(biteSizedActivityEntity.getEndDate());
        campaign.setActive(true);
        campaign.setAllowReporting(true);
        campaign.setClientSponsored(biteSizedActivityEntity.isClientSponsored());
        campaign.setAdverseEvent(false);
    	campaign.setBatchedAdverseEvent(false);
    	campaign.setHumanReadableUrl(biteSizedActivityTitle.toLowerCase());
    	campaign.setPhaseInt(biteSizedActivityEntity.getPhase().getValue());
    	campaign.setArchived(biteSizedActivityEntity.isArchived());
     	campaign.setPhase(biteSizedActivityEntity.getPhase());
     	
    	return campaign;
    }
    
    private BzzActivityEntity setActivity(BiteSizedActivityEntity biteSizedActivityEntity,BzzActivityEntity activity, CampaignEntity campaign){
    	
    	final String biteSizedActivityTitle = biteSizedActivityEntity.getTitle();
        Validate.notNull(biteSizedActivityTitle, "title is null");
        Validate.notNull(biteSizedActivityEntity.getStartDate(), "start date is null");
        Validate.notNull(biteSizedActivityEntity.getEndDate(), "end date is null");
        
    	activity.setCampaignEntity(campaign);
 		activity.setProduct_type_id((short) ProductType.Campaign.getValue());
 		activity.setTitle(biteSizedActivityTitle);
 		activity.setStartDate(biteSizedActivityEntity.getStartDate());
 		activity.setEndDate(biteSizedActivityEntity.getEndDate());
 		activity.setActivityExampleImageUrl(biteSizedActivityEntity.getActivityExampleImageUrl());
 		activity.setDescription(biteSizedActivityEntity.getActivityCopy());
 		activity.setHashtag(biteSizedActivityEntity.getHashtag());
 		activity.setPublish_to_facebook(biteSizedActivityEntity.isFacebookShareEnabled());
 		activity.setPublish_to_twitter(biteSizedActivityEntity.isTwitterShareEnabled());
 		activity.setProductType(ProductType.Campaign);
 		activity.setStatus(Status.Active);
 		activity.setHas_sentiment(false);
 		activity.setSentiment_only(false);
 		activity.setRequiresBucketing(false);
 		activity.setPriority((short) 1);
        activity.setMaxPerAgent(1);
 		
 		BzzActivityType activityType = biteSizedActivityEntity.getBzzActivityType();
 		if (activityType != null) {
 	 		activity.setBzzactivity_type_id((short)activityType.getValue());
 		}
 		
		activity.setMetadata(biteSizedActivityEntity.getMetadata());
		activity.setFacebookPostType(biteSizedActivityEntity.getFacebookPostType());
		
 		return activity;
    }

    
	@Override
	public BiteSizedActivityEntity getByIdent(String ident) {
		BiteSizedActivityEntity bsb = null;
		try {
			bsb = getBiteSizedBzzDao().getByIdent(ident);
			if (bsb!=null && bsb.getBzzActivityType().equals(BzzActivityType.Ecommerce)) {
				final Set<EcommerceEntity> ecommerce = getBzzActivityDao().getActivityEcommerce(ident);
				bsb = new BiteSizedActivityEntity.Builder(bsb)
		    	    .withEcommerceRetailersEntity(ecommerce)
		    	    .build();
			}
		} catch (NoResultException e) {
			LOG.debug("No results for bsb ident("+ident+")", e);
		} catch (RuntimeException e) {
			LOG.error("Exception getByIdent(ident:"+ident+")", e);
			throw e;
		} finally {
			close();
		}
		return bsb;
	}
	
	@Override
	public BiteSizedActivityEntity getById(Integer bzzactivityId) {
		BiteSizedActivityEntity bsb = null;
		try {
			bsb = getBiteSizedBzzDao().getById(bzzactivityId);
		} catch (NoResultException e) {
			LOG.info("No results for bzzactivityId("+bzzactivityId+")", e);
		} catch (RuntimeException e) {
			LOG.info("Exception getById(bzzactivityId:"+bzzactivityId+")", e);
			throw e;
		} finally {
			close();
		}
		return bsb;
	}

	@Override
    public List<BiteSizedActivityEntity> search(BiteSizedActivitySearchParams params) {
        try {
            return getBiteSizedBzzDao().search(params);
        } finally {
            close();
        }
    }

    @Override
    public void handleLaunch(Long bzzactivityId) {
    	Validate.notNull(bzzactivityId);
    	BiteSizedActivityEntity bsb = this.getById(bzzactivityId.intValue());
    	
    	// validation business rules
    	if (bsb == null) {
    		LOG.warn("BSB(bzzactivityId:"+bzzactivityId+") is null");
    		return;
    	}
    	LOG.info("handling bsb launch for " + bsb);
    	if (bsb.getCampaignEntity() == null) {
    		LOG.warn("BSB("+bsb+").campaignEntity is null");
    		return;
    	}
    	if (BzzUtil.isEmpty(bsb.getCampaignEntity().getCampaignProfiles()) ) {
    		LOG.warn("BSB("+bsb+").campaignEntity(id:"+bsb.getCampaignEntity().getId()+").campaignProfiles is empty");
    		return;
    	}
    	if (bsb.getCampaignEntity().getCampaignProfiles().size() > 1) {
    		LOG.warn("BSB("+bsb+").campaignEntity(id:"+bsb.getCampaignEntity().getId()+").campaignProfiles.size("+bsb.getCampaignEntity().getCampaignProfiles().size()+") greater than 1");
    		return;
    	}
    	if (!Phase.Launch.equals(bsb.getPhase())) {
    		LOG.warn("BSB("+bsb+").phase("+bsb.getPhase()+") is not Launch");
    		return;
    	}
    	if (!bsb.getCampaignEntity().isCurrentlyRunning()) {
    		LOG.warn("BSB("+bsb+").campaignEntity(id:"+bsb.getCampaignEntity().getId()+") is not running");
    		return;
    	}

    	// assemble the elements for handling the BSB event
    	CampaignProfileEntity campaignProfile = bsb.getCampaignEntity().getCampaignProfiles().iterator().next();
    	int profileId = campaignProfile.getId();
    	int memberGroupId = campaignProfile.getMemberGroupEntity().getId();
    	int campaignId = campaignProfile.getCampaignEntity().getId();
    	
    	this.handleBsbEvent(campaignId, profileId, memberGroupId);
    	
    	// TODO - if BSB are cached, this is the place to handle finding those members affecting and clearing their cache
    	// CampaignInviteManager ciMgr = new CampaignInviteModel();
		// Set<CampaignInviteBO> invites = ciMgr.findAll(new Long(campaignId), null);
    }
    
    @Override
    public void handleGroupBuild(Long groupId) {
    	// get the bzzactivities related to the member group
        String sql = String.format(
                "select ba.id as value from bzzactivity ba, campaign c, campaignprofile cp " +
                "where cp.cp_groupid = %d " +
                "and c.id = cp.cp_campaignid " +
                "and c.c_type = 5 " +
                "and ba.product_id = c.id;",
                groupId);

    	List<String> bzzactivityIdStrs = new GenericSQLBean().getMultiValues(sql);
    	for (String bzzactivityIdStr : bzzactivityIdStrs) {
    		this.handleLaunch(StringUtils.safeToLong(bzzactivityIdStr, -1L));
    	}
    }

    private void handleBsbEvent(int campaignId, int profileId, int memberGroupId) {
		String nowDate = StringUtils.safeDateToString(new Date(), StringUtils.YYYYMMDD_hhmmssSSS);
		
    	// revoke members that should not be previewed or invited
    	String sqlRevoke = String.format(
    			"update campaign_invite set status=%d, lastmodified='%s' "
    			+ "where campaign_id=%d and member_id not in (select memberid from xrefmembergroup where membergroupid=%d) "
    			+ "and status in (%d,%d)",
    			CampaignInviteStatus.Revoked.getValue(), nowDate,
    			campaignId, memberGroupId,
    			CampaignInviteStatus.Preview.getValue(), CampaignInviteStatus.Invited.getValue());
    	LOG.info("BSB Revoke(campaignId:"+campaignId+"): " + sqlRevoke);
    	
    	// preview members that were revoked
    	String sqlPreview = String.format(
    			"update campaign_invite set status=%d, lastmodified='%s' "
    			+ "where campaign_id=%d and member_id in (select memberid from xrefmembergroup where membergroupid=%d) "
    			+ "and status = %d",
    			CampaignInviteStatus.Preview.getValue(), nowDate,
    			campaignId, memberGroupId,
    			CampaignInviteStatus.Revoked.getValue());
    	LOG.info("BSB Preview(campaignId:"+campaignId+"): " + sqlPreview);
    	
    	String sqlNewPreview = String.format(
    			"insert into campaign_invite (member_id, campaign_id, profile_id, type, flight, status, date_issued, lastmodified, email_status) "
    			+ "  select x.memberid, %d, %d, 1, 1, 1, now(), now(), 0 from xrefmembergroup x where membergroupid=%d and x.memberid not in " 
    			+ " (select ci2.member_id from campaign_invite ci2 where ci2.campaign_id=%d and ci2.status in (%d,%d,%d))",
    			campaignId, profileId, memberGroupId,
    			campaignId, CampaignInviteStatus.Preview.getValue(), CampaignInviteStatus.Invited.getValue(), CampaignInviteStatus.Revoked.getValue());
    	LOG.info("BSB NewPreview(campaignId:"+campaignId+"): " + sqlNewPreview);
    	
    	
    	EntityManager em = null;
    	em = super.create();
    	try {
    		em.getTransaction().begin();
    		int numRevoke = em.createNativeQuery(sqlRevoke).executeUpdate();
    		int numPreview = em.createNativeQuery(sqlPreview).executeUpdate();
    		int numNewPreview = em.createNativeQuery(sqlNewPreview).executeUpdate();
    		em.getTransaction().commit();
    		LOG.info("Revoked("+numRevoke+"), Preview("+numPreview+"), NewPreview("+numNewPreview+")");
    		
    	} catch (RuntimeException e) {
    		LOG.info("BSB Exception handleBsbEvent(campaignId:"+campaignId+")", e);
    		if (em.getTransaction().isActive()) {
    			try {
    				em.getTransaction().rollback();
    			} catch (Exception ee) {
    				LOG.info("BSB Exception handleBsbEvent trying to rollback active transaction", ee);
    			}
    		}
    		throw e;
    	} finally {
    		super.close();
    	}
    }
	
    @Override
	public boolean getBsbAgentActivities(String username) {
    	if (StringUtils.isEmpty(username)) {
			return false;
		}
        try {
            return getBiteSizedBzzDao().memberHasBSBActivities(username);
        } finally {
            close();
        }
	}
    
	@Override
	public GetBsbAgentActivitiesResponse getBsbAgentActivities(BsbAgentActivitySearchParams request) {
		if (request == null) {
			return null;
		}
		final String username = request.getUsername();
		Validate.notNull(username);

		final List<BsbAgentActivityEntity> bsbActivities;
		try {
			bsbActivities = getBiteSizedBzzDao().getBsbAgentActivities(request);
		} finally {
			close();
		}
		final GetBsbAgentActivitiesResponse.Builder responseBuilder = new GetBsbAgentActivitiesResponse.Builder();

		return responseBuilder.withActivities(bsbActivities)
								.withTotalActivityCount(bsbActivities.size()).build();
	}

    @Override
    public MemberBiteSizedActivityStatusResponse updateInviteStatus(MemberBiteSizedActivityStatusRequest request) {
        if (request == null) {
            return null;
        }
        
        //get activity
        final BiteSizedActivityEntity activity = getByIdent(request.getIdent());
        if (activity == null) {
			return new MemberBiteSizedActivityStatusResponse.Builder().withActivityNotFound().build();
        }

		BsbActivityStatus activityStatus = BsbActivityStatus.findByName(request.getStatus());
		if (activityStatus == null) {
			return new MemberBiteSizedActivityStatusResponse.Builder().withInvalidStatus().build();
		}

		CampaignInviteStatus inviteStatus = CampaignInviteStatus.from(activityStatus);
		if (inviteStatus == null) {
			return new MemberBiteSizedActivityStatusResponse.Builder().withInvalidStatus().build();
		}

		return this.updateInviteStatus(activity.getCampaignEntity(), request.getMemberId(), inviteStatus);

	}
    
    @Override
    public MemberBiteSizedActivityStatusResponse updateInviteStatus(CampaignEntity campaignEntity, int memberId, CampaignInviteStatus inviteStatus) {
        //find campaign Invite

        EntityManager em = create();
        try {
            //find campaign Invite
            CampaignInviteEntity invite = getCampaignDao().findCampaignInviteByMemberAndCampaign(memberId, campaignEntity.getId());
            if (invite == null) {
                return new MemberBiteSizedActivityStatusResponse.Builder()
                        .withInviteNotFound()
                        .build();
            }
    
            if (inviteStatus == null) {
                return new MemberBiteSizedActivityStatusResponse.Builder()
                        .withInvalidStatus()
                        .build();
            }
            //update status
            invite.setStatus(inviteStatus);
            invite.setDateTaken(new Date().toInstant());
    
         // save everything
			em.getTransaction().begin();
			em.merge(invite);
            em.getTransaction().commit();
    	} finally {
			close();
		}
        
        return new MemberBiteSizedActivityStatusResponse.Builder().build();
    }


}
