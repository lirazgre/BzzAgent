--identifies actvities available for displaying on a member's profile page.
DROP VIEW IF EXISTS view_bsb_activity;
CREATE OR REPLACE VIEW view_bsb_activity AS
    SELECT b.id AS activity_id,
           b.ident AS activity_ident,
           b.bzzactivity_type_id,
           b.title,
           c.startdate,
           c.enddate,
           b.description AS activity_copy,
           b.activity_example_image_url,
           profile.cp_groupid AS group_id,
           b.hashtag,
           b.publish_to_facebook,
           b.publish_to_twitter,
           b.is_moderated,
           b.moderation_limit,
           b.moderation_qualities,
           b.facebook_post_type,
           b.metadata,
           b.journal_history,
           b.instructions AS moderation_instructions,
           CASE WHEN cp.gallery_visibility = 1 THEN true ELSE false END AS gallery_view_enabled,
           c.client_sponsored,
           c.archived,
           c.c_phase,
           CASE WHEN c.c_phase = 6 THEN (SELECT COUNT(id) FROM campaign_invite WHERE campaign_id = c.id AND status = 2) ELSE 0 END AS viewed_count,
           CASE WHEN c.c_phase = 6 THEN (SELECT COUNT(id) FROM campaign_invite WHERE campaign_id = c.id AND status = 4) ELSE 0 END AS dismissed_count,
           CASE WHEN c.c_phase = 6 THEN (SELECT COUNT(id) FROM campaign_invite WHERE campaign_id = c.id AND status = 5) ELSE 0 END AS completed_count,
           c.id AS campaign_id
    FROM bzzactivity b
    JOIN campaign c on c.id = b.product_id
    JOIN campaignprofile profile on profile.cp_campaignid = c.id
    JOIN xref_campaign_permissions cp on cp.campaign_id = c.id
   WHERE c.c_type = 5 /* campaign type is bite-sized-bzz */
;
