package product.orders.inventoryservice.application.saga;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.orders.inventoryservice.application.InventoryApplicationService;
import product.orders.inventoryservice.messaging.event.OrderCreatedEvent;
import product.orders.inventoryservice.messaging.event.OrderItem;

/**
 * Service responsible for reserving all items in an order within a transaction.
 */
@Service
public class InventoryReservationTxService {

    private final InventoryApplicationService inventoryApplicationService;

    public InventoryReservationTxService(InventoryApplicationService inventoryApplicationService) {
        this.inventoryApplicationService = inventoryApplicationService;
    }

    /**
     * Reserves all items in an order within a transaction, throwing an exception if any reservation fails.
     * @param event The order creation event containing the items to reserve.
     */
    @Transactional(value = "transactionManager")
    public void reserveAllItemsOrThrow(OrderCreatedEvent event) {
        for (OrderItem orderItem : event.items()) {
            inventoryApplicationService.reserve(
                    event.orderId(),
                    orderItem.productId(),
                    orderItem.quantity()
            );
        }
    }
}