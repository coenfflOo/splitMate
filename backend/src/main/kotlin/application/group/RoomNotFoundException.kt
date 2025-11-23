package application.group

class RoomNotFoundException(roomId: RoomId) :
    IllegalArgumentException("Room not found: ${roomId.value}")