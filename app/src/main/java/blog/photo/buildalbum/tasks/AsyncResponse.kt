package blog.photo.buildalbum.tasks

/**
 * Interface for async responses
 */
interface AsyncResponse {
    /**
     * Method to mark task begin execution.
     */
    fun onTaskBegin()

    /**
     * Method to mark task complete execution.
     */
    fun onTaskComplete(stringId: Int)
}