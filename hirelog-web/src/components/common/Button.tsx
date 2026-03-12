import React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '../../utils/cn';

const buttonVariants = cva(
    "inline-flex items-center justify-center rounded-full font-bold transition-all focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none active:scale-[0.98]",
    {
        variants: {
            variant: {
                primary: "bg-gradient-to-r from-[#0f172a] via-[#1e293b] to-[#0f172a] text-white hover:shadow-lg hover:shadow-slate-900/20 border border-transparent bg-[length:200%_200%] hover:bg-right transition-all duration-500", // Deep rich gradient from image
                secondary: "bg-white text-gray-900 border border-gray-200 hover:bg-gray-50 hover:border-gray-300 shadow-sm",
                outline: "bg-transparent border border-gray-300 text-gray-600 hover:border-[#276db8] hover:text-[#276db8] hover:bg-[#276db8]/5", // Pill style update
                ghost: "bg-transparent text-gray-600 hover:bg-gray-100",
                gradient: "bg-gradient-to-r from-[#276db8] to-[#89cbb6] text-white hover:shadow-lg hover:shadow-[#89cbb6]/20 border border-transparent",
            },
            size: {
                sm: "h-10 px-5 text-xs tracking-widest uppercase", // Adjusted for "design feel"
                md: "h-12 px-8 text-sm tracking-widest uppercase",
                lg: "h-14 px-10 text-base tracking-widest uppercase",
                icon: "h-10 w-10",
            },
            fullWidth: {
                true: "w-full",
            },
        },
        defaultVariants: {
            variant: "primary",
            size: "md",
            fullWidth: false,
        },
    }
);

export interface ButtonProps
    extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
    isLoading?: boolean;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
    ({ className, variant, size, fullWidth, isLoading, children, ...props }, ref) => {
        return (
            <button
                className={cn(buttonVariants({ variant, size, fullWidth, className }))}
                ref={ref}
                disabled={isLoading || props.disabled}
                {...props}
            >
                {isLoading ? (
                    <span className="mr-2 h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                ) : null}
                {children}
            </button>
        );
    }
);

Button.displayName = "Button";
