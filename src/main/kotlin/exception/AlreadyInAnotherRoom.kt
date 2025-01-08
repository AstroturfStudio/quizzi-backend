package exception

import kotlinx.serialization.Serializable

/**
 * @author guvencenanguvenal
 */
@Serializable
class AlreadyInAnotherRoom : BusinessError("Your are already in another room!")