/*
 * Password Management Servlets (PWM)
 * http://www.pwm-project.org
 *
 * Copyright (c) 2006-2009 Novell, Inc.
 * Copyright (c) 2009-2017 The PWM Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


import { Component } from '../component';
import { IDocumentService } from 'angular';

@Component({
    bindings: {
        onClose: '&'
    },
    stylesheetUrl: require('ux/dialog.component.scss'),
    templateUrl: require('ux/dialog.component.html'),
    transclude: true
})
export default class DialogComponent {
    onClose: (() => void);
    onKeyDown: ((event) => void);

    static $inject = [ '$document' ];
    constructor(private $document: IDocumentService) {
        var self = this;

        this.onKeyDown = (event) => {
            if (event.keyCode === 27) { // ESC
                self.closeDialog();
            }
        };
    }

    $onInit(): void {
        this.$document.on('keydown', this.onKeyDown);
    }

    $onDestroy(): void {
        this.$document.off('keydown', this.onKeyDown);
    }

    closeDialog(): void {
        this.onClose();
    }
}