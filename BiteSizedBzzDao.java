/*
 * Copyright (c) 2003-2015 BzzAgent, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * BzzAgent, Inc. ("Confidential Information"). You shall not disclose
 * such Confidential Information and shall use it only in accordance
 * with the terms of the license agreement you entered into with BzzAgent.
 *
 * BzzAgent MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 */

package bzzAgent;

import java.util.List;

import com.bzzagent.common.service.bitesized.BiteSizedActivitySearchParams;
import com.bzzagent.common.service.bitesized.BsbAgentActivitySearchParams;
import com.bzzagent.domain.BiteSizedActivityEntity;
import com.bzzagent.domain.BsbAgentActivityEntity;

/**
 * Performs operations for bite-sized bzz.
 * 
 * @author lirazg
 *
 */
public interface BiteSizedBzzDao {

    /**
     * @param activityIdent identifies the {@link BiteSizedActivityEntity}
     * @return the {@link BiteSizedActivityEntity} with the ident
     */
    BiteSizedActivityEntity getByIdent(String activityIdent);
    
    /**
     * @param bzzactivityId identifies the {@link BiteSizedActivityEntity}
     * @return the {@link BiteSizedActivityEntity} with the ident
     */
    BiteSizedActivityEntity getById(Integer bzzactivityId);
    
    /**
     * @param params
     * @return List of {@link BiteSizedActivityEntity} matching the parameters
     */
    List<BiteSizedActivityEntity> search(BiteSizedActivitySearchParams params);
    
    /**
     * @param params determines which agent activities to return
     * @return List of {@link BsbAgentActivityEntity} matching the parameters
     */
    List<BsbAgentActivityEntity> getBsbAgentActivities(BsbAgentActivitySearchParams params);
  
    /**
     * @param check if user has bsb activities avialbale 
     * @return boolean
     */
    boolean memberHasBSBActivities(String username);
  
}
