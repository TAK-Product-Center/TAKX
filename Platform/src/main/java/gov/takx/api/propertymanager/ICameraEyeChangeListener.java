package gov.takx.api.propertymanager;

import gov.takx.api.globe.ICameraEye;

/**
 * This is a single method interface with a callback that is handed an ICameraEye. The instance provides access to the
 * new position of the camera eye, thus allowing a plugin to monitor the user's movement, and perhaps advise them that
 * they are moving the camera away from the action.
 *
 * @since 1.11
 */
public interface ICameraEyeChangeListener
{
    /**
     * Notification that the camera eye has moved.
     *
     * @param newPosition New position of the camera eye
     */
    void onCameraEyeChange(ICameraEye newPosition);
}
