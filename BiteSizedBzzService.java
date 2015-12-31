package bzzAgent;

import com.bzzagent.bo.CampaignInviteStatus;
import com.bzzagent.bzzactivity.BzzActivityBO;
import com.bzzagent.common.domain.BzzActivityEntity;
import com.bzzagent.common.domain.CampaignEntity;
import com.bzzagent.common.domain.CampaignInviteEntity;
import com.bzzagent.domain.BiteSizedActivityEntity;
import com.bzzagent.domain.BsbAgentActivityEntity;
import com.bzzagent.webapp.util.NotFoundException;

import java.util.List;

public interface BiteSizedBzzService {
    
    /**
     * Creates everything needed for a bite sized activity.
     * <ul>
     * <li> {@link CampaignEntity} </li>
     * <li> {@link BzzActivityEntity} </li>
     * <li> {@link CampaignInviteEntity} </li>
     * </ul>
     * @param biteSizedActivityRequest contains the values for the new bite sized activity
     * @return the ident
     */
    CreateBiteSizedActivityResponse create(BiteSizedActivityRequest biteSizedActivityRequest);
    
    /**
     * Updates the existing BiteSizedActivityEntity with the values passed in.
     * @param biteSizedActivityRequest contains the new values for the update
     * @throws NotFoundException if there is no activity matching the {@link BiteSizedActivityEntity}'s ident.
     */
    BiteSizedActivityResponse update(BiteSizedActivityRequest biteSizedActivityRequest);
        
    /**
     * Retrieves the bite-sized activity information by the ident.
     * @param ident the ident for the {@link BzzActivityBO} representing the bite-sized activity
     * @return the {@link BiteSizedActivityEntity}
     */
    BiteSizedActivityEntity getByIdent(String ident);
    
    /**
     * Retrieves the bite-sized activity information by the bzzactivity.id value.
     * @param id the id for the {@link BzzActivityBO} representing the bite-sized activity
     * @return the {@link BiteSizedActivityEntity}
     */
    BiteSizedActivityEntity getById(Integer bzzactivityId);
    
    /**
     * Retrieves {@link BiteSizedActivityEntity}'s matching the search parameters.
     * @param params identifies which {@link BiteSizedActivityEntity} to return
     * @return the list of matching {@link BiteSizedActivityEntity}
     */
    List<BiteSizedActivityEntity> search(BiteSizedActivitySearchParams params);
    
    /**
     * When a BSB is launched, then we need to configure the invites. This will involve
     * creating or adjusting the invites based on the group membership.
     * 
     * @param bzzactivityId
     */
    void handleLaunch(Long bzzactivityId);

    /**
     * Whenever a group has been changed, this method will determine if its a BSB-related
     * group and is Launched and will adjust the invited agents.
     * 
     * @param groupId
     */
    void handleGroupBuild(Long groupId);
    
    /**
     * @param request parameters for selecting the user's bsb available activities
     * @return {@link GetBsbAgentActivitiesResponse } containing the {@link BsbAgentActivityEntity} or information about why the request failed.
     */
    GetBsbAgentActivitiesResponse getBsbAgentActivities(BsbAgentActivitySearchParams request);

    /**
     * @param check if user has bsb available activities
     * @return boolean 
     */
    boolean getBsbAgentActivities(String username);
    /**
     * Updates the status of a Bite-sized activity for a member.
     * This will update the status of the campaign invite for the member.
     *
     * @param request
     * @return {@link MemberBiteSizedActivityStatusResponse}
     */
    MemberBiteSizedActivityStatusResponse updateInviteStatus(MemberBiteSizedActivityStatusRequest request);
    MemberBiteSizedActivityStatusResponse updateInviteStatus(CampaignEntity campaignEntity, int memberId, CampaignInviteStatus inviteStatus);

}
