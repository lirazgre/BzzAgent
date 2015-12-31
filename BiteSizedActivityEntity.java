package bzzAgent;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import jersey.repackaged.com.google.common.collect.Sets;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import com.bzzagent.apps.core.model.ActivityIncentive;
import com.bzzagent.apps.core.model.EcommerceRepresentation;
import com.bzzagent.apps.core.model.JournalItem;
import com.bzzagent.apps.core.model.ModerationConfig;
import com.bzzagent.bzzactivity.BzzActivityMetadata;
import com.bzzagent.bzzactivity.BzzActivityType;
import com.bzzagent.common.domain.ActivityIncentiveEntity;
import com.bzzagent.common.domain.BzzActivityEntity;
import com.bzzagent.common.domain.CampaignEntity;
import com.bzzagent.common.domain.CampaignPermissionsEntity;
import com.bzzagent.common.domain.EcommerceEntity;
import com.bzzagent.common.enums.FacebookPostType;
import com.bzzagent.domain.BiteSizedActivityEntity;
import com.bzzagent.domain.BiteSizedActivityEntity.Builder;
import com.bzzagent.webapp.beans.CampaignBean;
import com.bzzagent.webapp.beans.CampaignBean.Phase;
import com.bzzagent.webapp.beans.CampaignScopeBean.GalleryVisibility;
import com.bzzagent.webapp.util.BzzUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author lirazg
 *
 */

@Entity
@Table(name="view_bsb_activity")
@EntityListeners({BiteSizedActivityEntity.BiteSizedActivityEntityListener.class})
public class BiteSizedActivityEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="activity_id")
    private BzzActivityEntity bzzActivityEntity;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="campaign_id")
    private CampaignEntity campaignEntity;
    
    @Id
    @Column(name="activity_ident")
    private String activityIdent;

    @Column(name="bzzactivity_type_id", insertable=false, updatable=false)
    private int bzzActivityTypeId;

    @Column(name="title", insertable=false, updatable=false)
    private String title;

    @Column(name="startdate", insertable=false, updatable=false)
    private LocalDate startDate;

    @Column(name="enddate", insertable=false, updatable=false)
    private LocalDate endDate;

    @Column(name="activity_copy", insertable=false, updatable=false)
    private String activityCopy;

    @Column(name="activity_example_image_url", insertable=false, updatable=false)
    private String activityExampleImageUrl;

    @Column(name="group_id", insertable=false, updatable=false)
    private Integer groupID;
    
    @Column(name="client_sponsored", insertable=false, updatable=false)
    private Boolean clientSponsored;

    @Column(name="is_moderated", insertable=false, updatable=false)
    private boolean isModerated;
    
    @Column(name="moderation_limit", insertable=false, updatable=false)
    private Integer moderationLimit = null;
    
    @Column(name="moderation_qualities", insertable=false, updatable=false)
    private String limitComdevQualities;

    @Column(name="moderation_instructions", insertable=false, updatable=false)
    private String moderationInstructions = null;

    @Column(name="hashtag", insertable=false, updatable=false)
    private String hashtag;

    @Column(name="gallery_view_enabled", insertable=false, updatable=false)
    private Boolean galleryViewEnabled;

    @Column(name="publish_to_facebook", insertable=false, updatable=false)
    private Boolean facebookShareEnabled;

    @Column(name="publish_to_twitter", insertable=false, updatable=false)
    private Boolean twitterShareEnabled;
    
    @Column(name = "archived")
    private boolean             archived;

    @Column(name = "c_phase")
	private int phaseInt;

    @Column(name = "metadata")
	private String				metadata;

    @Transient
	private BzzActivityMetadata bzzActivityMetadata;
    
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "facebook_post_type", columnDefinition = "FACEBOOK_POST_TYPE_ENUM")
	@Type(type = "com.bzzagent.common.enums.PGEnumUserType", parameters = { @Parameter(name = "enumClassName", value = "com.bzzagent.common.enums.FacebookPostType") })
	private FacebookPostType facebookPostType;
	
	@OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name="bzzactivity_id", referencedColumnName = "activity_id")
    private List<ActivityIncentiveEntity> incentives = new ArrayList<>();
	
	@Column(name="journal_history")
    private String journalHistoryJson;
    
	@Transient
    private List<JournalItem> journalItems;
 
    @Transient
    private Set<EcommerceEntity> ecommerceRetailers;

    @Column(name="viewed_count", insertable=false, updatable=false)
    private Integer viewedCount = null;

    @Column(name="dismissed_count", insertable=false, updatable=false)
    private Integer dismissedCount = null;

    @Column(name="completed_count", insertable=false, updatable=false)
    private Integer completedCount = null;

	private BiteSizedActivityEntity() {
        
    }
    private BiteSizedActivityEntity(Builder builder) {
        activityIdent = builder.activityIdent;
        bzzActivityEntity = builder.bzzActivityEntity;
        campaignEntity = builder.campaignEntity;
        bzzActivityTypeId = builder.bzzActivityType.getValue();
        title = builder.title;
        startDate = builder.startDate;
        endDate = builder.endDate;
        activityCopy = builder.activityCopy;
        activityExampleImageUrl = builder.activityExampleImageUrl;
        groupID = builder.groupID;
        clientSponsored = builder.clientSponsored;

		isModerated = builder.moderationConfig.isModerationEnabled();
		moderationLimit = builder.moderationConfig.getLimit();
		moderationInstructions = builder.moderationConfig.getInstructions();
		limitComdevQualities = builder.moderationConfig.getLimitComdevQualitiesString();
              
        hashtag = builder.hashtag;
        galleryViewEnabled = builder.galleryViewEnabled;
        facebookShareEnabled = builder.facebookShareEnabled;
        twitterShareEnabled = builder.twitterShareEnabled;
        archived = builder.archived;
        phaseInt = builder.phase.getValue();
        incentives = builder.incentives == null ? null : Collections.unmodifiableList(builder.incentives);
        ecommerceRetailers = builder.ecommerceRetailers == null ? null : Collections.unmodifiableSet(builder.ecommerceRetailers);
        metadata = builder.metadata;
        bzzActivityMetadata = builder.bzzActivityMetadata;
        facebookPostType = builder.facebookPostType;
        journalItems = builder.journalItems == null ? null : Collections.unmodifiableList(builder.journalItems);
        viewedCount = builder.viewedCount;
        dismissedCount = builder.dismissedCount;
        completedCount = builder.completedCount;
    }
    

	public String getMetadata() {
		return metadata;
	}
	
    public FacebookPostType getFacebookPostType() {
		return facebookPostType;
	}

    public String getActivityIdent() {
        return activityIdent;
    }

    public BzzActivityEntity getBzzActivityEntity() {
		return bzzActivityEntity;
	}
    
	public CampaignEntity getCampaignEntity() {
		return campaignEntity;
	}
	public BzzActivityType getBzzActivityType() {
        return BzzActivityType.findByValue(bzzActivityTypeId);
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getActivityCopy() {
        return activityCopy;
    }

    public String getActivityExampleImageUrl() {
        return activityExampleImageUrl;
    }

    public Integer getGroupID() {
        return groupID;
    }

    public BzzActivityMetadata getBzzActivityMetadata() {
		return bzzActivityMetadata;
	}
	public Boolean isClientSponsored() {
        return clientSponsored;
    }

    public ModerationConfig getModerationConfig() {
        final ModerationConfig moderationConfig = new ModerationConfig();
        moderationConfig.setModerationEnabled(isModerated);
        moderationConfig.setLimit(moderationLimit);
        moderationConfig.setInstructions(moderationInstructions);
        moderationConfig.setLimitComdevQualitiesString(limitComdevQualities);
        return moderationConfig;
    }

    public String getHashtag() {
        return hashtag;
    }

    public Phase getPhase() {
		return Phase.findByValue(phaseInt);
	}
    
    public boolean isLaunched() {
    	return CampaignBean.Phase.Launch.equals(this.getPhase());
    }
    
	public Boolean isGalleryViewEnabled() {
        return galleryViewEnabled;
    }

    public Boolean isFacebookShareEnabled() {
        return facebookShareEnabled;
    }

    public Boolean isTwitterShareEnabled() {
        return twitterShareEnabled;
    }

    public boolean isArchived() {
        return archived;
    }
    
    public List<ActivityIncentiveEntity> getIncentives() {
 		return incentives;
 	}

    public List<JournalItem> getJournalItems() {
        return journalItems;
    }
    
    public Set<EcommerceEntity> getEcommerceRetailers() {
		return ecommerceRetailers;
	}

    public Integer getViewedCount() {
        return viewedCount;
    }

    public Integer getDismissedCount() {
        return dismissedCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public static class Builder {
        String activityIdent;
        BzzActivityType bzzActivityType;
        CampaignEntity campaignEntity;
        BzzActivityEntity bzzActivityEntity;
        String title;
        LocalDate startDate;
        LocalDate endDate;
        String activityCopy;
        String activityExampleImageUrl;
        Integer groupID;
        Boolean clientSponsored;
        ModerationConfig moderationConfig;
        String hashtag;
        Boolean galleryViewEnabled;
        Boolean facebookShareEnabled;
        Boolean twitterShareEnabled;
        boolean archived;
        Phase phase;
        List<ActivityIncentiveEntity> incentives = new ArrayList<>();
        Set<EcommerceEntity> ecommerceRetailers =  Sets.newHashSet();
        List<JournalItem> journalItems = new ArrayList<>();
        String metadata;
        FacebookPostType facebookPostType;
        BzzActivityMetadata bzzActivityMetadata;
        Integer viewedCount;
        Integer dismissedCount;
        Integer completedCount;
        
        public Builder() {
            
        }
        
        @SuppressWarnings("synthetic-access")
        public BiteSizedActivityEntity build() {
            return new BiteSizedActivityEntity(this);
        }
        
        public Builder(BiteSizedActivityEntity source) {
            activityIdent = source.getActivityIdent();
            bzzActivityEntity = source.getBzzActivityEntity();
            campaignEntity = source.getCampaignEntity();
            bzzActivityType = source.getBzzActivityType();
            title = source.getTitle();
            startDate = source.getStartDate();
            endDate = source.getEndDate();
            activityCopy = source.getActivityCopy();
            activityExampleImageUrl = source.getActivityExampleImageUrl();
            groupID = source.getGroupID();
            clientSponsored = source.isClientSponsored();
            moderationConfig = source.getModerationConfig();
            hashtag = source.getHashtag();
            galleryViewEnabled = source.isGalleryViewEnabled();
            facebookShareEnabled = source.isFacebookShareEnabled();
            twitterShareEnabled = source.isTwitterShareEnabled();
            archived = source.isArchived();
            phase = source.getPhase();
            incentives = source.getIncentives();
            metadata = source.getMetadata();
            facebookPostType = source.getFacebookPostType();
            bzzActivityMetadata = source.getBzzActivityMetadata();
            journalItems = source.getJournalItems();
            ecommerceRetailers = source.getEcommerceRetailers();
            viewedCount = source.getViewedCount();
            dismissedCount = source.getDismissedCount();
            completedCount = source.getCompletedCount();
        }
                
        public Builder withBzzActivityMetadata(BzzActivityMetadata newValue) {
        	this.bzzActivityMetadata = newValue;
    	    this.metadata = newValue == null ? null : newValue.getJsonText();
            return this;
        }
        
        public Builder withMetadata(String newValue) {
        	this.metadata = newValue;
        	this.bzzActivityMetadata = new BzzActivityMetadata(newValue);
            return this;
        }

        public Builder withFacebookPostType(FacebookPostType type) {
        	this.facebookPostType = type;
            return this;
        }
        
        public Builder withPhase(Phase newValue) {
        	this.phase = newValue;
            return this;
        }
        
        public Builder withActivityIdent(String newValue) {
            activityIdent = newValue;
            return this;
        }
        
        public Builder withBzzActivityType(BzzActivityType newValue) {
            bzzActivityType = newValue;
            return this;
        }
        
        public Builder withTitle(String newValue) {
            title = newValue;
            return this;
        }

        public Builder withStartDate(LocalDate newValue) {
            startDate = newValue;
            return this;
        }

        public Builder withEndDate(LocalDate newValue) {
            endDate = newValue;
            return this;
        }
        
        public Builder withActivityCopy(String newValue) {
            activityCopy = newValue;
            return this;
        }

        public Builder withActivityExampleImageUrl(String newValue) {
            activityExampleImageUrl = newValue;
            return this;
        }
        
        public Builder withGroupId(Integer newValue) {
            groupID = newValue;
            return this;
        }
        
        public Builder withModerationConfig(ModerationConfig newValue) {
            moderationConfig = newValue;
            return this;
        }
        public Builder withClientSponsored(Boolean newValue) {
            clientSponsored = newValue;
            return this;
        }
        public Builder withHashtag(String newValue) {
            hashtag = newValue;
            return this;
        }
        public Builder withGalleryViewEnabled(Boolean newValue) {
            galleryViewEnabled = newValue;
            return this;
        }
        public Builder withFacebookShareEnabled(Boolean newValue) {
            facebookShareEnabled = newValue;
            return this;
        }
        public Builder withTwitterShareEnabled(Boolean newValue) {
            twitterShareEnabled = newValue;
            return this;
        }
        public Builder withArchived(boolean newValue) {
            archived = newValue;
            return this;
        }
        
        public Builder withCampaign(CampaignEntity campaign) {
            startDate = campaign.getAutoLaunchDate();
            endDate =  campaign.getEndDate();
            clientSponsored = campaign.isClientSponsored();
            archived = campaign.getArchived();
            phase = campaign.getPhase();
            campaignEntity = campaign;
            return this;
        }
        
        public Builder withActivityIncentiveEntity(List<ActivityIncentiveEntity> newValues) {
        	incentives = new ArrayList<>();
        	this.incentives.addAll(newValues);
            return this;
        }        

        public Builder withActivityIncentive(List<ActivityIncentive> newValues) {
            incentives = new ArrayList<>();
        	for(ActivityIncentive incentive: newValues){
        		ActivityIncentiveEntity entity = new ActivityIncentiveEntity(incentive);
        		incentives.add(entity);
        	}
            return this;
        }
        
        public Builder withEcommerceRetailers(Set<EcommerceRepresentation> newValues) {
        	ecommerceRetailers = Sets.newHashSet();
         	for(EcommerceRepresentation ecom: newValues){
         		EcommerceEntity entity = new EcommerceEntity(ecom);
         		ecommerceRetailers.add(entity);
         	}
             return this;
        }
        
        public Builder withEcommerceRetailersEntity(Set<EcommerceEntity> items) {
        	this.ecommerceRetailers = Sets.newHashSet();
            if(BzzUtil.isNotEmpty(items)){
            	this.ecommerceRetailers.addAll(items);
            }
            return this;
        }
        
        public Builder withJournalItems(List<JournalItem> items) {
            this.journalItems = items;
            return this;
        }
        
        public Builder withJournalItem(JournalItem journalItem) {
            journalItems.add(journalItem);
            return this;
        }
        
        public Builder withCampaignPermissions(CampaignPermissionsEntity campaignPermissionsEntity) {
            galleryViewEnabled = campaignPermissionsEntity.getGalleryVisibility() == GalleryVisibility.Public;
            return this;
        }

        public Builder withViewedCount(Integer count) {
            this.viewedCount = count;
            return this;
        }

        public Builder withDismissedCount(Integer count) {
            this.dismissedCount = count;
            return this;
        }

        public Builder withCompletedCount(Integer count) {
            this.completedCount = count;
            return this;
        }
       
        public Builder withBzzActivityEntity(BzzActivityEntity bzzActivityBO) {
            
            activityIdent = bzzActivityBO.getIdent();
            this.bzzActivityEntity = bzzActivityBO;
            bzzActivityType = bzzActivityBO.getBzzActivityType();
            title = bzzActivityBO.getTitle();
            activityCopy = bzzActivityBO.getInstructions();
            activityExampleImageUrl = bzzActivityBO.getActivityExampleImageUrl();
            activityCopy = bzzActivityBO.getDescription();
            hashtag = bzzActivityBO.getHashtag();
            
            facebookShareEnabled = bzzActivityBO.getPublishToFacebook();
            twitterShareEnabled = bzzActivityBO.getPublishToTwitter();
            
            moderationConfig = new ModerationConfig();
            moderationConfig.setModerationEnabled(bzzActivityBO.isModerated());
            moderationConfig.setInstructions(bzzActivityBO.getInstructions());
            moderationConfig.setLimit(bzzActivityBO.getModerationLimit());
            moderationConfig.setLimitComdevQualitiesString(bzzActivityBO.getModerationQualities());
            
            metadata = bzzActivityBO.getMetadata();
            facebookPostType = bzzActivityBO.getFacebookPostType();
            bzzActivityMetadata =  new BzzActivityMetadata(bzzActivityBO.getMetadata());
            journalItems = bzzActivityBO.getJournalItems();
            ecommerceRetailers = bzzActivityBO.getEcommerces();
            return this;
        }
    }
    
    /**
     * JPA lifecycle listener class
     * 
     * @author fblesso
     * @date 2015-04-14
     *
     */
    public static class BiteSizedActivityEntityListener implements Serializable {

        private static final long serialVersionUID = 1L;

        @PrePersist
        public void prePersist(BiteSizedActivityEntity entity) { //NO_UCD
        	if(entity.getBzzActivityMetadata()!=null){
				entity.metadata = entity.getBzzActivityMetadata().getJsonText();
			}
        }

		@PostLoad
		public void postLoad( BiteSizedActivityEntity entity) {
			if(entity.getMetadata()!=null){
				entity.bzzActivityMetadata = new BzzActivityMetadata(entity.getMetadata());
			}
            if (entity.journalHistoryJson != null) {
                final ObjectMapper mapper = new ObjectMapper();
                try {
                    final List<JournalItem> journalItems = mapper.readValue(entity.journalHistoryJson, 
                            mapper.getTypeFactory().constructCollectionType(List.class, JournalItem.class));
                    entity.journalItems = Collections.unmodifiableList(journalItems);
                } catch (IOException e) {
                    throw new RuntimeException("error deserializing journal items:" + entity.journalHistoryJson, e);
                }
            }
            
		}

    }
    
}
